"""Validate HTTP request shape without sending requests to physical equipment."""
import io
import json
import unittest
import urllib.error
import urllib.parse
from unittest.mock import patch

from manager.common import RemoteProblem
from manager.proxmox import PveClient


class PveTransportTests(unittest.TestCase):
    def setUp(self):
        self.api=object.__new__(PveClient)
        self.api.base='https://pve.invalid:8006/api2/json'
        self.api.auth='PVEAPIToken=test-only'
        self.api.context=object()
        self.api.timeout=20

    def request(self,method,path,body=None):
        with patch('manager.proxmox.urllib.request.urlopen',return_value=io.BytesIO(b'{"data":"UPID:fixture"}')) as call:
            self.assertEqual('UPID:fixture',self.api.call(method,path,body))
        self.assertIs(self.api.context,call.call_args.kwargs['context'])
        return call.call_args.args[0]

    def test_delete_parameters_use_query_without_content_body(self):
        request=self.request('DELETE','/nodes/pve01/qemu/10000',{'purge':1,'destroy-unreferenced-disks':1})
        self.assertEqual('DELETE',request.method)
        self.assertIsNone(request.data)
        self.assertIsNone(request.get_header('Content-length'))
        self.assertEqual({'purge':['1'],'destroy-unreferenced-disks':['1']},
                         urllib.parse.parse_qs(urllib.parse.urlsplit(request.full_url).query))

    def test_existing_query_and_lists_are_preserved(self):
        request=self.request('DELETE','/fixture?existing=1',{'value':['a b','x&y']})
        self.assertEqual({'existing':['1'],'value':['a b','x&y']},
                         urllib.parse.parse_qs(urllib.parse.urlsplit(request.full_url).query))
        self.assertIsNone(request.data)

    def test_post_and_put_keep_form_body(self):
        for method in ['POST','PUT']:
            request=self.request(method,'/fixture',{'password':'fixture-only a&b'})
            self.assertEqual('',urllib.parse.urlsplit(request.full_url).query)
            self.assertEqual({'password':['fixture-only a&b']},urllib.parse.parse_qs(request.data.decode()))

    def test_get_parameters_use_query(self):
        request=self.request('GET','/fixture',{'path':'/etc/machine-id'})
        self.assertIsNone(request.data)
        self.assertEqual({'path':['/etc/machine-id']},urllib.parse.parse_qs(urllib.parse.urlsplit(request.full_url).query))

    def test_empty_delete_has_no_body(self):
        for body in [None,{}]:
            self.assertIsNone(self.request('DELETE','/fixture',body).data)

    def test_http_error_retains_status_without_echoing_body(self):
        error=urllib.error.HTTPError('https://pve.invalid',501,'Unexpected content',{},
                                     io.BytesIO(b'private-response-value'))
        with patch('manager.proxmox.urllib.request.urlopen',side_effect=error):
            with self.assertRaises(RemoteProblem) as caught:self.api.call('DELETE','/fixture',{'purge':1})
        self.assertEqual('PVE_HTTP_ERROR',caught.exception.code)
        self.assertEqual(501,caught.exception.status)
        self.assertNotIn('private-response-value',str(caught.exception))


if __name__=='__main__':unittest.main()
