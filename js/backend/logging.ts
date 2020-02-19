
import winston from "winston";


/** Main app logger object, managed by Winston. */
const appLogger = winston.createLogger({
    level: 'info',
    format: winston.format.simple(),
    transports: [
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        })
    ]
});


/**
 * Emit a log line at the specified level, with the specified arguments.
 *
 * @param {!LogLevel} level Level at which to emit this log line.
 * @param {!string} message Message to emit as part of this log line.
 * @param {!Array<!string>} context Context args to emit as part of this log line.
 */
function doLog(level: LogLevel, message: string, context: any[]) {
    appLogger.log(level, message, context);
}


/**
 * Enumerates log levels available for the core Node logger.
 */
export enum LogLevel {
    /** Verbose-level log lines. */
    DEBUG = 'debug',

    /** Informational log lines. */
    INFO = 'info',

    /** Warnings/non-fatal error states. */
    WARN = 'warn',

    /** Errors and exceptions. */
    ERROR = 'error'
}


/**
 * Logging facade. Exports an interface by which global logging tools may be accessed statically. Simply invoke the
 * method corresponding to your desired log level.
 */
export class Logging {
    /** Emit a regular log line. */
    public static log(message: string, ...args: any[]) {
        doLog(LogLevel.DEBUG, message, args);
    }

    /** Emit an `info` log line. */
    public static info(message: string, ...args: any[]) {
        doLog(LogLevel.INFO, message, args);
    }

    /** Emit a `warn`-level log line. */
    public static warn(message: string, ...args: any[]) {
        Logging.warning(message, args);
    }

    /** Emit a `warn`-level log line. */
    public static warning(message: string, ...args: any[]) {
        doLog(LogLevel.WARN, message, args);
    }

    /** Emit an `error`-level log line. */
    public static error(message: string, ...args: any[]) {
        doLog(LogLevel.ERROR, message, args);
    }
}
