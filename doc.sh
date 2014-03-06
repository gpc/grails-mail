#!/bin/bash
# This relies on these environment variables being set:
# TRAVIS_REPO_SLUG
# TRAVIS_BUILD_ID
# GIT_NAME
# GIT_EMAIL
# GIT_TOKEN

./grailsw doc --pdf &&
git config --global user.name "$GIT_NAME" &&
git config --global user.email "$GIT_EMAIL" &&
git config --global credential.helper "store --file=~/.git-credentials" &&
echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials &&
git clone https://${GH_TOKEN}@github.com/$TRAVIS_REPO_SLUG.git -b gh-pages gh-pages --single-branch > /dev/null &&
cd gh-pages &&
git rm -rf . &&
cp -r ../target/docs/. ./ &&
git add * &&
git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID" &&
git push origin HEAD &&
cd .. &&
rm -rf gh-pages
