import os
import re

import requests

gh_repo = os.environ["GITHUB_REPOSITORY"]
gh_token = os.environ["GITHUB_TOKEN"]

headers = {
    'Authorization': f'Bearer {gh_token}',
    'Accept': 'application/vnd.github.v3+json',
}


def get_latest_release():
    url = f'https://api.github.com/repos/{gh_repo}/releases/latest'
    response = requests.get(url, headers=headers)

    if not 200 <= response.status_code <= 299:
        return

    return response.json()


def mark_build_attached(is_attached):
    with open('build_attached', 'w') as file:
        file.write("1" if is_attached else "0")
        file.flush()
        file.close()


if __name__ == "__main__":
    aar_path = 'stash/build/outputs/aar/stash-release.aar'
    if not os.path.exists(aar_path) or not os.path.isfile(aar_path):
        print(f"Cannot find build package {aar_path}")
        mark_build_attached(False)
        exit(1)

    latest_release = get_latest_release()
    if not latest_release:
        print(f"There is no latest release, exiting")
        mark_build_attached(False)
        exit(0)

    print(f"Attaching {aar_path} to release {latest_release['name']} as 'release.aar'")

    upload_url = latest_release['upload_url'].split('{', 1)[0] + f'?name=release.aar'

    print(f"Uploading to {upload_url}")

    with open(aar_path, 'rb') as file_data:
        headers['Content-Type'] = 'application/vnd.android.package-archive'
        attach_response = requests.post(upload_url, headers=headers, data=file_data)
        attach_response.raise_for_status()
        print(f"Success")

    mark_build_attached(True)
