# coding: utf-8

"""
Identus Cloud Agent

No description provided

The version of the OpenAPI document: 1.0.0
Generated by: https://openapi-generator.tech
"""

from setuptools import setup, find_packages  # noqa: H301

NAME = "cloud-agent-client-python"
VERSION = "0.0.1"

REQUIRES = [
    "certifi >= 14.5.14",
    "frozendict ~= 2.3.4",
    "python-dateutil ~= 2.7.0",
    "setuptools >= 21.0.0",
    "typing_extensions ~= 4.3.0",
    "urllib3 ~= 1.26.7",
]

setup(
    name=NAME,
    version=VERSION,
    description="Identus Cloud Agent Client",
    author="Allain Magyar",
    author_email="",
    url="https://github.com/hyperledger-identus/cloud-agent",
    keywords=["Identus Cloud Agent Client"],
    python_requires=">=3.7",
    install_requires=REQUIRES,
    packages=find_packages(exclude=["test", "tests"]),
    include_package_data=True,
    long_description="""\
    Identus Cloud Agent models generated from the OpenAPI Specification
    """,
)
