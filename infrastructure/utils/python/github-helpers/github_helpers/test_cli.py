"""Testing module for GitHub helpers"""

import os

from click.testing import CliRunner

# pylint: disable=E0402
from .cli import cli


def test_download_arts_positive():
    """Download arts test"""
    runner = CliRunner()
    result = runner.invoke(
        cli, ["download-arts", "--branch", "master", "--run-id", "latest"]
    )
    assert result.exit_code == 0, "Error during download-arts command"
    if not os.path.exists("app-debug.apk"):
        assert False, "Application was not downloaded!"
    else:
        os.remove("app-debug.apk")


def test_download_arts_negative():
    """Negative test for arts downloading"""
    runner = CliRunner()
    result = runner.invoke(
        cli, ["download-arts", "--branch", "master", "--run-id", "never-exist"]
    )
    assert result.exit_code == 1, "Arts downloaded but not exist!"


def test_get_latest_version():
    """Latest version test"""
    runner = CliRunner()
    result = runner.invoke(cli, ["get-latest-package-version"])
    assert result.exit_code == 0, "Unable to get latest package version!"
    assert result.output != "", "Latest version cannot be empty string!"
