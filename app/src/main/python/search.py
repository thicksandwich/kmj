import json
from jamdict import Jamdict

def lookup(word):
    jam = Jamdict()
    result = jam.lookup(word)
    json_results = []
    for entry in result.entries:
        jmd_dict = entry.to_dict()
        json_results.append(jmd_dict)
    return json.dumps(json_results)