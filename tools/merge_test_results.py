#!/usr/bin/env python

import sys
import shutil
import logging
import colorlog

from os import path
from junitparser import JUnitXml


loglevel = logging.DEBUG


# colorized logger
result_colors = {
    "pass": "green",
    "fail": "red"
}

handler = colorlog.StreamHandler(sys.stderr)
formatter = colorlog.ColoredFormatter(
    "%(log_color)s%(levelname)-8s%(message)s%(reset)s",
    datefmt=None,
    reset=True,
    log_colors={
        'DEBUG':    'cyan',
        'INFO':     'green',
        'WARNING':  'yellow',
        'ERROR':    'red',
        'CRITICAL': 'red,bg_white',
    },
    style='%'
)

logger = colorlog.getLogger()
logger.setLevel(loglevel)
handler.setFormatter(formatter)
logger.addHandler(handler)


def merge_reports(reports, out):

    """Parses each JUnit result and merges it into one."""

    tests = 0
    suites = 0
    passed_tests = 0
    failed_tests = 0
    merged_suites = []
    suites_by_name = {}
    merged = JUnitXml()
    with open(out, "w"):
        for report in reports:
            logger.info("Parsing '%s'" % report)
            report_path = path.realpath(report)

            try:
                report_xml = JUnitXml.fromfile(report_path)
                for suite in report_xml:
                    suites += 1
                    cases = [case for case in suite]
                    tests += len(cases)

                    suite_name = suite.name
                    for case in cases:
                        case_name = case.name
                        if suite_name == case_name:
                            # splice in a better suite & case name, if needed
                            segments = []
                            subject = path.split(report)[0]

                            # walk the directory tree until we hit the test base,
                            # in order to determine an appropriate suite name
                            while subject:
                                (subject, segment) = path.split(subject)
                                if segment == "tests" or segment == "javatests" or segment == "jstests":
                                    break
                                else:
                                    segments.append(segment)
                            segments = [i for i in reversed(segments)]
                            suite_name = ".".join(segments[0:-1])
                            case_name = segments[-1]
                            suite.name = suite_name
                            case.name = case_name

                        passed = case.result is not False
                        line = ("- Test result '%s:%s': " % (suite_name, case_name))
                        line += passed and "PASSED" or "FAILED"
                        logger.debug(line)
                        if passed:
                            passed_tests += 1
                        else:
                            failed_tests += 1

                        if suite_name in suites_by_name:
                            suites_by_name[suite_name].add_testcase(case)
                        else:
                            merged_suites.append(suite)
                            suites_by_name[suite_name] = suite

            except RuntimeError as e:
                logger.error("FATAL ERROR: Failed to parse report at path '%s'. Error: '%s'." % (report, e))
                sys.exit(3)

        map(merged.add_testsuite, merged_suites)

        logger.info("Merged %s tests across %s suites (%s passed, %s failed)..." % (
            tests, suites, passed_tests, failed_tests))
        merged.write(path.realpath(out))


def main():

    """Run the tool, merging any provided JUnit files."""

    if len(sys.argv) < 2:
        logger.error("Please provide the output file as the first argument, if you want to merge test results.")
        sys.exit(2)

    elif len(sys.argv) < 3:
        logger.error("Please provide files to merge.")
        sys.exit(2)

    elif len(sys.argv) == 3:
        logger.warning("No need to merge: one result provided. Copying...")
        out, ins = (sys.argv[1], sys.argv[-1])
        shutil.copyfile(ins, out)
        logger.info("Report copied at '%s'." % out)

    else:
        out, ins = (sys.argv[1], sys.argv[2:])
        logger.info("Merging %s test reports..." % len(ins))
        merge_reports(ins, out)
        logger.info("Report merged and written at '%s'." % out)


if __name__ == "__main__": main()
