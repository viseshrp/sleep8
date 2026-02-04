SHELL := /bin/bash

GRADLEW ?= ./gradlew
ANDROID_SDK_ROOT ?= $(HOME)/Library/Android/sdk
ADB_SERIAL ?=
TEST_FILTER ?=

.PHONY: check lint test-unit test-integration test-ui coverage all

lint:
	@echo "Running static checks (if configured)"
	@if $(GRADLEW) -q tasks --all | grep -q "^detekt"; then $(GRADLEW) detekt; else echo "detekt task not configured"; fi
	@if $(GRADLEW) -q tasks --all | grep -q "ktlintCheck"; then $(GRADLEW) ktlintCheck; else echo "ktlintCheck task not configured"; fi

check: lint test-unit

all: check

TEST_ARGS :=
ifneq ($(strip $(TEST_FILTER)),)
TEST_ARGS := --tests "$(TEST_FILTER)"
endif

test-unit:
	ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT) $(GRADLEW) testDebugUnitTest $(TEST_ARGS)

test-integration:
	ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT) $(GRADLEW) testDebugUnitTest --tests "com.sleep8.integration.*"

test-ui:
	ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT) ANDROID_SERIAL=$(ADB_SERIAL) $(GRADLEW) connectedDebugAndroidTest

coverage:
	ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT) $(GRADLEW) jacocoTestReport jacocoTestCoverageVerification
