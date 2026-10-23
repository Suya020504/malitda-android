"""Malitda v2 meaning generation. Local fake provider only; never calls a paid API."""
import base64
import io

from fastapi.testclient import TestClient
from PIL import Image
import pytest

import image_api
import style_profiles


class FakeProvider:
    ready = True

    def __init__(self):
        self.calls = []
        out = io.BytesIO()
        Image.new('RGB', (3, 2), 'green').save(out, format='PNG')
        self.data_url = 'data:image/png;base64,' + base64.b64encode(out.getvalue()).decode()

    def generate(self, prompt):
        self.calls.append(prompt)
        return {'data_url': self.data_url, 'provider': 'fake-local', 'model': 'test-only'}


@pytest.fixture
def setup(tmp_path, monkeypatch):
    monkeypatch.setattr(style_profiles, 'DB_PATH', tmp_path / 'profiles.sqlite3')
    monkeypatch.setenv('OPENROUTER_API_KEY', '')
    provider = FakeProvider()
    client = TestClient(image_api.create_app(provider), base_url='http://127.0.0.1:8769')
    token = client.get('/api/config').json()['session_token']
    headers = {'origin': 'http://127.0.0.1:8769', 'x-demo-token': token}
    assert client.post('/api/profiles/anon-0001/style', headers=headers,
                       json={'style_id': 'watercolor', 'expected_revision': 0}).status_code == 200
    return client, provider, headers


def body(**extra):
    b = {'request_id': 'meaning-0001', 'description': '화장실에 가고 싶어요', 'consent': True,
         'profile_id': 'anon-0001', 'profile_revision': 1, 'meaning': '화장실에 가고 싶어요'}
    b.update(extra)
    return {k: v for k, v in b.items() if v is not None}


def test_meaning_uses_student_style_and_is_not_stored_as_scene(setup):
    client, provider, headers = setup
    result = client.post('/api/generate', headers=headers, json=body())
    assert result.status_code == 200, result.text
    payload = result.json()
    assert payload['style_id'] == 'watercolor'
    assert payload['scene'] is None
    assert payload['review_status'] == 'pending'
    assert len(provider.calls) == 1
    assert 'watercolor' in provider.calls[0] and '화장실에 가고 싶어요' in provider.calls[0]
    cards = client.get('/api/profiles/anon-0001/cards').json()['cards']
    assert all(c['data_url'] is None for c in cards)


def test_meaning_stale_revision_rejected_before_provider(setup):
    client, provider, headers = setup
    assert client.post('/api/profiles/anon-0001/style', headers=headers,
                       json={'style_id': 'clay', 'expected_revision': 1}).status_code == 200
    assert client.post('/api/generate', headers=headers, json=body()).status_code == 409
    assert not provider.calls


def test_meaning_requires_profile_and_forbids_scene(setup):
    client, provider, headers = setup
    assert client.post('/api/generate', headers=headers, json=body(profile_revision=None)).status_code == 422
    assert client.post('/api/generate', headers=headers, json=body(request_id='meaning-0002', scene='walk')).status_code == 422
    assert client.post('/api/generate', headers=headers, json=body(request_id='meaning-0003', meaning='x' * 121)).status_code == 422
    assert not provider.calls
