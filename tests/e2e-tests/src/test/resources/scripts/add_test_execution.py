#!/usr/bin/env python

import click
import json

@click.command()
@click.argument('file_path', type=click.Path(exists=True))
@click.argument('new_tag')
def add_tag(file_path, new_tag):
    # read the JSON object from the file
    with open(file_path, 'r') as file:
        json_obj = json.load(file)

    # add the new tag to the "tags" field of each scenario
    for scenario in json_obj[0]["elements"]:
        scenario["tags"].append({"name": new_tag})

    # write the updated JSON object back to the file
    with open(file_path, 'w') as file:
        json.dump(json_obj, file)

    # print a message to indicate that the operation is complete
    click.echo(f'The "{new_tag}" tag has been added to the JSON object in the file "{file_path}".')

if __name__ == '__main__':
    add_tag()
