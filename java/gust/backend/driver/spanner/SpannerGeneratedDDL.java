package gust.backend.driver.spanner;

import com.google.cloud.spanner.Type;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.protobuf.Descriptors.Descriptor;
import static com.google.protobuf.Descriptors.FieldDescriptor;
import static gust.backend.driver.spanner.SpannerUtil.*;
import static gust.backend.model.ModelMetadata.*;
import static java.lang.String.format;
import static java.lang.String.join;


/** Container for generated schema-driven Spanner DDL. */
@Immutable
@ThreadSafe
public final class SpannerGeneratedDDL {
    /** Represents a DDL statement structure in code that can be rendered down to a string. */
    public interface RenderableStatement {
        /**
         * Render this statement into a String buffer.
         *
         * @return Rendered statement.
         */
        @Nonnull StringBuilder render();
    }

    /** Sort direction settings which can apply to columns. */
    public enum SortDirection {
        /** Sort values in the column in ascending order. This is the default value. */
        ASC,

        /** Sort values in the column in descending order. */
        DESC
    }

    /** Specifies options for reference action propagation (i.e. on-delete or on-update). */
    public enum PropagatedAction {
        /** Take no action. This is the default value. */
        NO_ACTION,

        /** Cascade changes on delete or update. */
        CASCADE
    }

    /** Specifies a generic table constraint to include in a DDL statement. */
    public static final class TableConstraint implements RenderableStatement {
        final @Nonnull String name;
        final @Nonnull String expression;

        /**
         * Private constructor for a table constraint specification.
         *
         * @param name Name of the table constraint.
         * @param expression Expression to use as a constraint.
         */
        private TableConstraint(@Nonnull String name, @Nonnull String expression) {
            this.name = name;
            this.expression = expression;
        }

        /**
         * Spawn a table constraint at the provided name, enforcing the provided expression.
         *
         * @param name Name of the constraint to enclose in the DDL statement.
         * @param expression Expression to enforce as a table constraint.
         * @return Table constraint specification.
         */
        public static @Nonnull TableConstraint named(@Nonnull String name,
                                                     @Nonnull String expression) {
            return new TableConstraint(name, expression);
        }

        /** @inheritDoc */
        @Override
        public @Nonnull StringBuilder render() {
            return (new StringBuilder()).append(format(
                "CONSTRAINT %s CHECK ( %s )",
                this.name,
                this.expression
            ));
        }
    }

    /** Specify a parent table against which this table is interleaved. */
    public static final class InterleaveTarget implements RenderableStatement {
        final @Nonnull String parent;
        final @Nonnull Optional<PropagatedAction> action;

        /**
         * Private constructor for an interleave target for a Spanner table.
         *
         * @param parent Parent table where we should interleave this table.
         * @param action Action to take, if any, when deletes or changes happen in the parent table.
         */
        private InterleaveTarget(@Nonnull String parent,
                                 @Nonnull Optional<PropagatedAction> action) {
            this.parent = parent;
            this.action = action;
        }

        /**
         * Generate an interleave target specification for the provided parent table name.
         *
         * @see #forParent(String, Optional) To pass a propagation action.
         * @param parent Parent table where we should interleave a given table.
         * @return Interleave target specification.
         */
        public static @Nonnull InterleaveTarget forParent(@Nonnull String parent) {
            return forParent(parent, Optional.empty());
        }

        /**
         * Generate an interleave target specification for the provided parent table name, optionally applying the
         * provided propagation action.
         *
         * @param parent Parent table where we should interleave a given table.
         * @param action Action to take, if any, when parent rows change that should affect the child table.
         * @return Interleave target specification.
         */
        public static @Nonnull InterleaveTarget forParent(@Nonnull String parent,
                                                          @Nonnull Optional<PropagatedAction> action) {
            return new InterleaveTarget(parent, action);
        }

        /** @inheritDoc */
        @Override
        public @Nonnull StringBuilder render() {
            var buf = new StringBuilder(format(
                "INTERLEAVE IN PARENT %s",
                this.parent
            ));

            if (this.action.isPresent()) {
                buf.append(" ");
                buf.append(format(
                    "ON DELETE %s",
                    this.action.get().name()
                ));
            }

            return buf;
        }
    }

    /** Specifies an individual field as part of a DDL statement. */
    private static final class ColumnSpec implements RenderableStatement {
        final @Nonnull String name;
        final @Nonnull Type type;
        final @Nonnull Integer length;
        final @Nonnull FieldDescriptor field;
        final @Nonnull Boolean nonnull;
        final @Nonnull Optional<String> expression;
        final @Nonnull Boolean expressionStored;
        final @Nonnull Boolean allowCommitTimestamp;

        /**
         * Private constructor.
         *
         * @param name Column name in Spanner.
         * @param type Type specification in Spanner.
         * @param length Length for string fields, or `0`.
         * @param nonnull Whether to mark the field as non-null.
         * @param allowCommitTimestamp Whether to fill this column with the commit timestamp.
         * @param field Model field that spawned this column specification.
         */
        ColumnSpec(@Nonnull String name,
                   @Nonnull Type type,
                   @Nonnull Integer length,
                   @Nonnull Optional<Boolean> nonnull,
                   @Nonnull Optional<String> expression,
                   @Nonnull Optional<Boolean> expressionStored,
                   @Nonnull Optional<Boolean> allowCommitTimestamp,
                   @Nonnull FieldDescriptor field) {
            this.name = name;
            this.type = type;
            this.length = length;
            this.nonnull = nonnull.orElse(false);
            this.expression = expression;
            this.expressionStored = expressionStored.orElse(false);
            this.allowCommitTimestamp = allowCommitTimestamp.orElse(false);
            this.field = field;
        }

        /**
         * Create a column spec for the provided field information, considering any active driver settings.
         *
         * @param fieldPointer Pointer to the field we should consider.
         * @param settings Active set of Spanner driver settings.
         * @return Spawned column corresponding to the provided field.
         */
        static @Nonnull ColumnSpec columnSpecForField(@Nonnull FieldPointer fieldPointer,
                                                      @Nonnull SpannerDriverSettings settings) {
            var columnOpts = columnOpts(fieldPointer);
            var spannerOpts = spannerOpts(fieldPointer);
            var fieldName = resolveColumnName(fieldPointer, spannerOpts, columnOpts, settings);
            var fieldType = resolveColumnType(fieldPointer, spannerOpts, columnOpts, settings);
            Type innerType = fieldPointer.getField().isRepeated() ?
                    fieldType.getArrayElementType() :
                    fieldType;
            var fieldSize = innerType.getCode() == Type.Code.STRING ?
                    resolveStringColumnSize(fieldPointer.getField(), spannerOpts, columnOpts, settings) :
                    -1;

            return new ColumnSpec(
                fieldName,
                fieldType,
                fieldSize,
                Optional.empty(),  // @TODO(sgammon): value for this
                Optional.empty(),  // @TODO(sgammon): value for this
                Optional.empty(),  // @TODO(sgammon): value for this
                Optional.empty(),  // @TODO(sgammon): value for this
                fieldPointer.getField()
            );
        }

        /**
         * Create a column spec for the provided model key field, considering any active driver settings.
         *
         * @param model Model schema for the object or key record.
         * @param keyField Primary key field pre-resolved for a given Spanner table.
         * @param settings Active Spanner driver settings.
         * @return Spawned primary key column corresponding to the provided model key.
         */
        static @Nonnull ColumnSpec columnSpecForKey(@Nonnull Descriptor model,
                                                    @Nonnull FieldPointer keyField,
                                                    @Nonnull SpannerDriverSettings settings) {
            var idField = idField(model).orElseThrow();
            var keyName = resolveKeyColumn(keyField, settings);
            var keyType = resolveKeyType(idField);
            var spannerOpts = spannerOpts(idField);
            var columnOpts = columnOpts(idField);
            int stringKeySize = -1;
            if (keyType.getCode() == Type.Code.STRING) {
                stringKeySize = resolveStringColumnSize(keyField.getField(), spannerOpts, columnOpts, settings);
            }

            return new ColumnSpec(
                keyName,
                keyType,
                stringKeySize,
                Optional.of(true),  // primary keys are always set to `NOT NULL`.
                Optional.empty(),  // primary keys do not support expressions
                Optional.empty(),
                Optional.empty(),  // primary keys cannot be set to the commit timestamp
                keyField.getField()
            );
        }

        /**
         * Render this column spec into a definition statement, suitable for use when creating a table.
         *
         * @return Rendered column spec statement.
         */
        @Override
        public @Nonnull StringBuilder render() {
            // prepare field statement
            var buf = new StringBuilder();

            // calculate field type designation first
            String fieldType;
            Type.Code innerType = this.field.isRepeated() ?
                    this.type.getArrayElementType().getCode() :
                    this.type.getCode();

            String innerTypeSpec = innerType == Type.Code.STRING ? format(
                "STRING(%s)",
                this.length
            ) : this.type.getCode().name();

            if (this.type.getCode() == Type.Code.ARRAY) {
                // it's a repeated field
                fieldType = format(
                    "ARRAY<%s>",
                    innerTypeSpec
                );
            } else {
                // it's a singular field. make sure to cover the special case for strings.
                fieldType = innerTypeSpec;
            }

            buf.append(format(
                "%s %s",
                this.name,
                fieldType
            ));

            // prepare field options
            var optionsBuffer = new ArrayList<String>();

            // consider NONNULL
            if (this.nonnull) {
                optionsBuffer.add("NOT NULL");
            }

            // consider expressions
            if (this.expression.isPresent()) {
                optionsBuffer.add(format("AS ( %s )", this.expression.get()));
                if (this.expressionStored)
                    optionsBuffer.add("STORED");
            }

            // consider options
            if (this.allowCommitTimestamp) {
                optionsBuffer.add("OPTIONS allow_commit_timestamp = true");
            }
            if (!optionsBuffer.isEmpty()) {
                buf.append(" ");
                buf.append(join(" ", optionsBuffer));
            }
            return buf;
        }
    }

    /**
     * Build properties for a generated Spanner table DDL statement, based on a given model instance as a base for
     * configuring the table name (via annotations / calculated defaults) and set of typed Spanner value columns.
     *
     * <p>To build the actual DDL statement, fill out the builder, build it, and then ask the resulting object for the
     * DDL as a string.</p>
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        /** Base model on which this builder will operate. Immutable. */
        final @Nonnull Descriptor model;

        /** Active set of driver settings. Immutable. */
        final @Nonnull SpannerDriverSettings settings;

        /** Immutable: Name of the table in Spanner. */
        final @Nonnull String tableName;

        /** Immutable: Name of the primary key column. */
        final @Nonnull String primaryKey;

        /** Immutable: Generated columns in Spanner. */
        final @Nonnull List<ColumnSpec> columns;

        /** Mutable: Key column sort direction. */
        @Nonnull SortDirection keySortDirection;

        /** Mutable: List of table constraints. */
        @Nonnull Optional<List<TableConstraint>> tableConstraints;

        /** Mutable: Optimizer version to apply. */
        @Nonnull Optional<Integer> optimizerVersion;

        /** Mutable: Version retention period. */
        @Nonnull Optional<String> versionRetentionPeriod;

        /** Mutable: Table interleave target. */
        @Nonnull Optional<InterleaveTarget> interleaveTarget;

        /**
         * Package-private constructor for a builder.
         *
         * @see SpannerGeneratedDDL#generateTableDDL(Descriptor, SpannerDriverSettings) to spawn one of
         *      these from regular library or application code.
         * @param model Descriptor for the model we are building against.
         * @param primaryKey Primary key field name to use for this table by default.
         * @param tableName Resolved table name to use for this table.
         * @param defaultColumns Default set of columns to use for this table.
         * @param settings Active driver settings to apply/consider.
         */
        Builder(@Nonnull Descriptor model,
                @Nonnull String tableName,
                @Nonnull String primaryKey,
                @Nonnull List<ColumnSpec> defaultColumns,
                @Nonnull SpannerDriverSettings settings) {
            this.model = model;
            this.tableName = tableName;
            this.settings = settings;
            this.columns = defaultColumns;
            this.primaryKey = primaryKey;
            this.keySortDirection = SortDirection.ASC;
            this.tableConstraints = Optional.empty();
            this.optimizerVersion = Optional.empty();
            this.versionRetentionPeriod = Optional.empty();
            this.interleaveTarget = Optional.empty();
        }

        /**
         * Render column definition statements for a final DDL table create statement.
         *
         * @return Column definition statements, stacked in a buffer.
         */
        @Nonnull String renderColumnStatements() {
            return this.columns.stream()
                    .map(ColumnSpec::render)
                    .collect(Collectors.joining(", "));
        }

        /**
         * Render table-level constraint statements for a final DDL table create statement.
         *
         * @return Any applicable rendered table constraints.
         */
        @Nonnull String renderConstraintStatements() {
            return this.tableConstraints.map(constraints -> constraints
                    .stream()
                    .map(TableConstraint::render)
                    .collect(Collectors.joining(", ")))
                    .orElse("");
        }

        /**
         * Render inner statements in the CREATE TABLE DDL statement, including columns and constraints, as applicable.
         * If no constraints are present, we simply return the column definitions alone.
         *
         * @return Rendered definitions of columns and table constraints.
         */
        @Nonnull String renderColumnStatementsAndConstraints() {
            var columnList = renderColumnStatements();
            if (this.tableConstraints.isPresent()) {
                var constraints = renderConstraintStatements();
                return format("%s, %s", columnList, constraints);
            }
            return columnList;
        }

        /**
         * Render the primary key specification for a final DDL table create statement.
         *
         * @return Rendered primary key specification.
         */
        @Nonnull String renderPrimaryKey() {
            return format(
                "%s %s",
                this.primaryKey,
                this.keySortDirection.name()
            );
        }

        /**
         * Render the prepared DDL statement details into a statement string which can be passed to Spanner.
         *
         * @return Rendered DDL statement, according to local object settings.
         */
        @Nonnull StringBuilder renderCreateDDLStatement() {
            var builder = new StringBuilder();
            var buf = new ArrayList<StringBuilder>();
            buf.add(new StringBuilder(format(
                "CREATE TABLE %s (%s) PRIMARY KEY (%s)",
                this.tableName,
                this.renderColumnStatementsAndConstraints(),
                this.renderPrimaryKey()
            )));

            // add interleave target statement, if specified
            this.interleaveTarget.ifPresent(target -> buf.add(target.render()));

//            this.rowDeletionPolicy.ifPresent(target -> buf.add(target.render()));

            builder.append(join(", ", buf));
            return builder;
        }

        /**
         * Collapse the builder into an immutable DDL statement container
         *
         * @return Immutable DDL statement container.
         */
        public @Nonnull SpannerGeneratedDDL build() {
            var fields = forEachField(
                model,
                Optional.of(onlySpannerEligibleFields(settings))
            ).map((fieldPointer) ->
                    ColumnSpec.columnSpecForField(fieldPointer, settings)
            ).collect(Collectors.toUnmodifiableList());

            return new SpannerGeneratedDDL(
                tableName,
                fields,
                model,
                renderCreateDDLStatement()
            );
        }

        // -- Builder API: Getters -- //

        /** @return Model descriptor this builder wraps. */
        public @Nonnull Descriptor getModel() {
            return model;
        }

        /** @return Active Spanner driver settings. */
        public @Nonnull SpannerDriverSettings getSettings() {
            return settings;
        }

        /** @return Generated or resolved Spanner table name. */
        public @Nonnull String getTableName() {
            return tableName;
        }

        /** @return Primary key column for this model/table. */
        public @Nonnull String getPrimaryKey() {
            return primaryKey;
        }

        /** @return Set of generated columns for this model in Spanner. */
        public @Nonnull List<ColumnSpec> getColumns() {
            return columns;
        }

        /** @return Primary key column sort direction. */
        public @Nonnull SortDirection getKeySortDirection() {
            return keySortDirection;
        }

        /** @return Set of constraints to apply to this table. */
        public @Nonnull Optional<List<TableConstraint>> getTableConstraints() {
            return tableConstraints;
        }

        /** @return Optimizer version to set for this table. */
        public @Nonnull Optional<Integer> getOptimizerVersion() {
            return optimizerVersion;
        }

        /** @return Data versioning retention period to set for this table. */
        public @Nonnull Optional<String> getVersionRetentionPeriod() {
            return versionRetentionPeriod;
        }

        /** @return Parent interleaving target for this table. */
        public @Nonnull Optional<InterleaveTarget> getInterleaveTarget() {
            return interleaveTarget;
        }

        // -- Builder API: Setters -- //

        /**
         * Set the sort direction for the primary key column in this table.
         *
         * @param keySortDirection Key column sort direction.
         * @return Self, for chained calls to the builder.
         */
        public @Nonnull Builder setKeySortDirection(@Nonnull SortDirection keySortDirection) {
            this.keySortDirection = keySortDirection;
            return this;
        }

        /**
         * Set, or clear, the set of table constraints added to this table.
         *
         * @param tableConstraints Desired table constraints to set or clear, as applicable.
         * @return Self, for chained calls to the builder.
         */
        public @Nonnull Builder setTableConstraints(@Nonnull Optional<List<TableConstraint>> tableConstraints) {
            this.tableConstraints = tableConstraints;
            return this;
        }

        /**
         * Set, or clear, the optimizer version to apply when creating this table.
         *
         * @param optimizerVersion Desired optimizer version to apply, as applicable.
         * @return Self, for chained calls to the builder.
         */
        public @Nonnull Builder setOptimizerVersion(@Nonnull Optional<Integer> optimizerVersion) {
            this.optimizerVersion = optimizerVersion;
            return this;
        }

        /**
         * Set, or clear, the data versioning retention period for this table.
         *
         * @param versionRetentionPeriod Desired data versioning retention period, as applicable.
         * @return Self, for chained calls to the builder.
         */
        public @Nonnull Builder setVersionRetentionPeriod(@Nonnull Optional<String> versionRetentionPeriod) {
            this.versionRetentionPeriod = versionRetentionPeriod;
            return this;
        }

        /**
         * Set, or clear, the parent interleave target for this table.
         *
         * @param interleaveTarget Desired parent interleave target, as applicable.
         * @return Self, for chained calls to the builder.
         */
        public @Nonnull Builder setInterleaveTarget(@Nonnull Optional<InterleaveTarget> interleaveTarget) {
            this.interleaveTarget = interleaveTarget;
            return this;
        }
    }

    /** Model that relates to this generated statement. */
    private final @Nonnull Descriptor model;

    /** Resolved name of the table. */
    private final @Nonnull String tableName;

    /** Set of generated columns determined to be part of this table. */
    private final @Nonnull List<ColumnSpec> columns;

    /** Holds the generated query in a string buffer. */
    private final @Nonnull StringBuilder generatedStatement;

    /**
     * Private constructor.
     *
     * @param tableName Name of the generated table.
     * @param columns Generated set of columns.
     * @param model Model this table corresponds to.
     * @param generatedStatement Rendered DDL statement.
     */
    private SpannerGeneratedDDL(@Nonnull String tableName,
                                @Nonnull List<ColumnSpec> columns,
                                @Nonnull Descriptor model,
                                @Nonnull StringBuilder generatedStatement) {
        this.tableName = tableName;
        this.columns = columns;
        this.generatedStatement = generatedStatement;
        this.model = model;
    }

    /**
     * Given a model definition, produce a generated DDL statement which creates a backing table in Spanner implementing
     * that model's properties. This method variant operates from a full model instance.
     *
     * @param instance Model instance to generate a table statement for.
     * @return Generated DDL statement object.
     */
    public static @Nonnull SpannerGeneratedDDL.Builder generateTableDDL(
            @Nonnull Message instance,
            @Nonnull Optional<SpannerDriverSettings> settings) {
        return generateTableDDL(
            instance.getDescriptorForType(),
            settings.orElse(SpannerDriverSettings.DEFAULTS)
        );
    }

    /**
     * Given a model definition, produce a generated DDL statement which creates a backing table in Spanner implementing
     * that model's properties.
     *
     * @param model Model schema to generate a table statement for.
     * @return Generated DDL statement object.
     */
    public static @Nonnull SpannerGeneratedDDL.Builder generateTableDDL(
            @Nonnull Descriptor model,
            @Nonnull SpannerDriverSettings settings) {
        return new SpannerGeneratedDDL.Builder(
            model,
            resolveTableName(model),
            resolveKeyColumn(keyField(model).orElseThrow(), settings),
            resolveDefaultColumns(model, settings),
            settings
        );
    }

    /**
     * Resolve the default calculated set of Spanner columns for a given model structure.
     *
     * @param model Model to traverse and generate columns for.
     * @return Set of generated and type-resolved columns.
     */
    public static @Nonnull List<ColumnSpec> resolveDefaultColumns(@Nonnull Descriptor model,
                                                                  @Nonnull SpannerDriverSettings settings) {
        var keyField = keyField(model).orElseThrow();
        var fieldSet = new LinkedList<ColumnSpec>();

        // first up: generate the column which implements the model's primary key
        fieldSet.add(ColumnSpec.columnSpecForKey(
            model,
            keyField,
            settings
        ));

        // next: generate all remaining data columns
        forEachField(
            model,
            Optional.of(onlySpannerEligibleFields(settings))
        ).filter((fieldPointer) ->
            // filter out key fields: we'll handle those separately
            !keyField.getField().getFullName().equals(fieldPointer.getField().getFullName())
        ).map((fieldPointer) ->
            ColumnSpec.columnSpecForField(fieldPointer, settings)
        ).forEach(fieldSet::add);

        return Collections.unmodifiableList(fieldSet);
    }

    // -- Accessors -- //

    /** @return Model for which this object generates a table create statement. */
    public @Nonnull Descriptor getModel() {
        return model;
    }

    /** @return Resolved name of the table to be created. */
    public @Nonnull String getTableName() {
        return tableName;
    }

    /** @return Resolved set of Spanner columns. */
    public @Nonnull List<ColumnSpec> getColumns() {
        return columns;
    }

    /** @return Rendered generated DDL statement. */
    public @Nonnull StringBuilder getGeneratedStatement() {
        return generatedStatement;
    }

    @Override
    public String toString() {
        return "SpannerDDL{" +
            "model=" + model.getFullName() +
            ", tableName='" + tableName + '\'' +
            ", statement=\"" + generatedStatement.toString() +
        "\"}";
    }
}
