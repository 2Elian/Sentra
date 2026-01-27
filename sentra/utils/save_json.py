import os
import json

def save_qa_pair(save_dir, results_aggregated, results_multihop, results_cot):
    os.makedirs(save_dir, exist_ok=True)
    save_path_aggregated = f"{save_dir}/aggregated.json"
    save_path_multihop = f"{save_dir}/multi_hop.json"
    save_path_cot = f"{save_dir}/cot.json"
    with open(save_path_aggregated, 'w', encoding='utf-8') as f:
        json.dump(results_aggregated, f, ensure_ascii=False, indent=2)
    with open(save_path_multihop, 'w', encoding='utf-8') as f:
        json.dump(results_multihop, f, ensure_ascii=False, indent=2)
    with open(save_path_cot, 'w', encoding='utf-8') as f:
        json.dump(results_cot, f, ensure_ascii=False, indent=2)
