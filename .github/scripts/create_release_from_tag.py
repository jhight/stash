import json
import os
import re

import requests

gh_repo = os.environ["GITHUB_REPOSITORY"]
gh_token = os.environ["GITHUB_TOKEN"]

headers = {
    'Authorization': f'Bearer {gh_token}',
    'Accept': 'application/vnd.github.v3+json',
}


def get_recent_version_tags():
    url = f'https://api.github.com/repos/{gh_repo}/tags'
    response = requests.get(url, headers=headers)

    if not 200 <= response.status_code <= 299:
        return

    results = response.json()

    # semver - e.g., "1.2.3"
    p = re.compile(r'^\d+\.\d+\.\d+$')

    tags = list()

    for tag in results:
        if p.match(tag['name']):
            tags.append(tag)

    return tags


def get_latest_release():
    url = f'https://api.github.com/repos/{gh_repo}/releases/latest'
    response = requests.get(url, headers=headers)

    if not 200 <= response.status_code <= 299:
        return

    return response.json()


def get_commits_between(end_sha, start_sha, max_commits=300):
    page = 1
    commits = list()
    start_found = False

    print(f"Looking for commits starting from {end_sha} back to {start_sha}")

    while True:
        if start_found or len(commits) >= max_commits:
            print(f"Finished looking")
            return commits

        print(f"Reviewing page {page} commits")
        url = f'https://api.github.com/repos/{gh_repo}/commits?sha={end_sha}&page={page}'
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        page_result = response.json()

        if len(page_result) == 0:
            print(f"Page {page} had no results, finished looking")
            return commits

        for commit in page_result:
            if commit['sha'] == start_sha:
                start_found = True
                break
            else:
                print(f"Adding {commit['sha']}")
                commits.append(commit)

        page += 1


def parse_commit_log(tag, previous_tag):
    print(f"Retrieving commits after {tag['commit']['sha'][:7]} and before {previous_tag['commit']['sha'][:7]}")
    commits = get_commits_between(tag['commit']['sha'], previous_tag['commit']['sha'])
    commit_log = '<ul>'
    for commit in commits:
        print(f"  - {commit['sha'][:7]} by @{commit['author']['login']}")
        commit_log += (f'<li><a href="{commit["html_url"]}"><code>{commit["sha"][:7]}</code></a> by '
                       f'@{commit["author"]["login"]}</li>')
    commit_log += f'</ul>'
    return commit_log


def create_release(tag, previous_tag):
    commit_log = parse_commit_log(tag, previous_tag)
    commit_log_heading = (f'<a href="https://github.com/{gh_repo}/compare/{previous_tag["name"]}...{tag["name"]}">'
                          f'Commits since {previous_tag["name"]}</a>')

    body = (f'<details><summary>{commit_log_heading}</summary>{commit_log}</details>')

    data = {
        'tag_name': tag['name'],
        'target_commitish': 'main',
        'name': tag['name'],
        'body': body,
    }

    print(f"Submitting release:\n{json.dumps(data, indent=2)}")

    url = f'https://api.github.com/repos/{gh_repo}/releases'
    response = requests.post(url, json=data, headers=headers)
    if 200 <= response.status_code <= 299:
        return response.json()
    else:
        print(f"Release creation failed (status {response.status_code}): {response.text}")


def mark_release_created(is_created):
    with open('release_created', 'w') as file:
        file.write("1" if is_created else "0")
        file.flush()
        file.close()


if __name__ == "__main__":
    recent_version_tags = get_recent_version_tags()
    latest_version_tag = recent_version_tags[0] if len(recent_version_tags) else None
    previous_version_tag = recent_version_tags[1] if len(recent_version_tags) > 1 else None

    if not latest_version_tag:
        print(f"Version tag not found, exiting")
        mark_release_created(False)
        exit(0)

    print(f"Latest version tag {latest_version_tag['name']} is {latest_version_tag['commit']['sha']}")
    print(f"Previous version tag {previous_version_tag['name']} is {previous_version_tag['commit']['sha']}")

    latest_release = get_latest_release()

    print(f"Latest release is {latest_release['tag_name'] if latest_release else 'undefined'}")

    if latest_release and latest_version_tag['name'] == latest_release['tag_name']:
        print(f"Latest version tag is identical to latest release, no need to do anything")
        mark_release_created(False)
        exit(0)

    print(f"Creating release {latest_version_tag['name']}")

    release = create_release(latest_version_tag, previous_version_tag)

    if not release:
        print(f"Unable to create release")
        mark_release_created(False)
        exit(1)

    print(f"Created:\n{json.dumps(release, indent=2)}")
    mark_release_created(True)
