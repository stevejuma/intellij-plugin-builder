.PHONY: plugin
plugin: ## Builds and generates the plugin distribution
	./gradlew buildPlugin

.PHONY: verify
verify: ## validates completeness and contents of plugin.xml descriptors as well as plugin's archive structure
	./gradlew verifyPlugin runPluginVerifier

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'