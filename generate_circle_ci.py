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
    store_artifacts:
      path: .
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

output_str += FOOTER

with open('./.circleci/config.yml', 'w', encoding='utf-8') as f:
    f.write(output_str)
