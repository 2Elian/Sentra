import os
import sys

current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.abspath(os.path.join(current_dir, '..'))
sentra_config_dir = os.path.join(project_root, 'configs', 'sentra')
sys.path.insert(0, sentra_config_dir)

try:
    from sentra_config import Config
    settings = Config.load_config()

except ImportError as e:
    import importlib.util
    spec = importlib.util.spec_from_file_location("settings", os.path.join(sentra_config_dir, "sentra_config.py"))
    sentra_config = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(sentra_config)
    settings = sentra_config.Config.load_config()