import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from manager.server import load_config

spec = importlib.util.spec_from_file_location('local_encoding_test', Path(__file__).resolve().parents[1] / 'scripts/local.py')
local = importlib.util.module_from_spec(spec)
spec.loader.exec_module(local)


class ConfigEncodingTests(unittest.TestCase):
    def config(self):
        return {'mode': 'PROXMOX', 'worker_id': 'encoding-test', 'server_ids': [57],
                'agent_token': 'a' * 32, 'worker_token': 'w' * 32,
                'state_path': 'C:/관리컴퓨터/state.sqlite3'}

    def test_load_utf8_and_bom_preserves_korean_path(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'config.json'
            for encoding in ['utf-8', 'utf-8-sig']:
                path.write_text(json.dumps(self.config(), ensure_ascii=False), encoding=encoding)
                self.assertEqual(self.config(), load_config(path))

    def test_start_reads_unicode_config_without_starting_duplicate(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'config.json'
            path.write_text(json.dumps(self.config(), ensure_ascii=False), encoding='utf-8-sig')
            with patch.object(local, 'CONFIG', path), patch.object(local, 'running', return_value=True) as running, \
                    patch.object(local.subprocess, 'Popen') as launch, contextlib.redirect_stdout(io.StringIO()):
                local.start()
            self.assertEqual(self.config(), running.call_args.args[0])
            launch.assert_not_called()

    def test_status_reads_unicode_config(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'config.json'
            path.write_text(json.dumps(dict(self.config(), port=19090), ensure_ascii=False), encoding='utf-8')
            with patch.object(local, 'CONFIG', path), \
                    patch.object(local.urllib.request, 'urlopen', return_value=io.BytesIO(b'{"status":"UP"}')), \
                    contextlib.redirect_stdout(io.StringIO()) as output:
                local.status()
            self.assertEqual({'status': 'UP'}, json.loads(output.getvalue()))


if __name__ == '__main__':
    unittest.main()
