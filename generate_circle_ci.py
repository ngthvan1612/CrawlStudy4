import os
import json

HEADER = """version: 2.1

commands:
  install_java_package:
    steps:
      - run:
          name: Cài đặt java package
          command: |
            sudo apt-get update
            sudo apt-get install maven openjdk-17-jdk -y
            sudo apt-get install redis
            sudo redis-server --daemonize yes
jobs:
"""

JOB_BASE = """
  crawl-{crawl_title}:
    docker:
      - image: cimg/base:2023.03
    environment:
      CRAWL_FROM: {crawl_from}
      CRAWL_TO: {crawl_to}
    steps:
      - install_java_package
      - checkout
      - run:
          name: "Crawl study {crawl_from} - {crawl_to} - {crawl_title}"
          command: |
            mvn clean install -DskipTests
            mvn test-compile compile
            mvn exec:java -Djava.util.concurrent.ForkJoinPool.common.parallelism=32
      - persist_to_workspace:
            path: ./
"""

FOOTER = """
workflows:
  crawl-study-4:
    jobs:
"""

TEMPLATE_JOB = """
      - crawl-{crawl_title}:
          filters:
            branches:
              only:
                - deployment
"""

DEPENDENCY_UPLOAD = """
      - upload-to-vps:
          requires:
"""

UPLOAD_TO_VPS = """
  upload-to-vps:
    docker:
      - image: cimg/base:2023.03
    steps:
      - run:
          name: "Upload to VPS"
          command: |
            pwd
            ls
            #sudo apt-get install git python3 python3-pip -y
      - persist_to_workspace:
            path: ./
"""

with open('./tests.json', 'r') as f:
    j = json.load(f)

output_str = HEADER

for i in range(0, 40, 2):
    fmt = JOB_BASE.format(
        crawl_from = i,
        crawl_to = i + 1,
        crawl_title = j[i]['slug'] + '-' + j[i + 1]['slug']
    )

    output_str += fmt

    FOOTER += TEMPLATE_JOB.format(
        crawl_title = j[i]['slug'] + '-' + j[i + 1]['slug']
    )

    DEPENDENCY_UPLOAD += "            - crawl-" + j[i]['slug'] + '-' + j[i + 1]['slug'] + '\n'

output_str += UPLOAD_TO_VPS
output_str += FOOTER + DEPENDENCY_UPLOAD

with open('./.circleci/config.yml', 'w', encoding='utf-8') as f:
    f.write(output_str)
