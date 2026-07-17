.PHONY: help install dev build clean release

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

install: ## Install npm dependencies
	npm install

dev: ## Start dev server met hot reload
	npx shadow-cljs watch app

build: ## Eenmalige dev compile
	npx shadow-cljs compile app

release: ## Optimized production build
	npx shadow-cljs release app

clean: ## Verwijder build artifacts
	rm -rf resources/public/js node_modules/.cache .shadow-cljs
