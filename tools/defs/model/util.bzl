##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

def _target_name(name, postfix):
    if "/" in name and ":" in name:
        # it's an absolute reference with full spec.
        return "%s:%s_%s" % (
            name.split(":")[0],
            name.split(":")[1],
            postfix,
        )
    elif "/" in name and ":" not in name:
        # it's an absolute reference with shorthand. resolve the last path portion and apply it as a target name.
        alias = (name.startswith("//") and name[2:] or name).split("/")[-1]
        return "%s:%s_%s" % (
            name,
            alias,
            postfix,
        )
    else:
        # it's a relative reference. safe to string-doctor and return.
        return "%s_%s" % (
            name,
            postfix,
        )

target_name = _target_name
