# hydra-sandbox

A macOS sandbox profile generator for [Claude Code](https://claude.ai/claude-code). It produces [SBPL (Sandbox Profile Language)](https://reverse.put.as/wp-content/uploads/2011/09/Apple-Sandbox-Guide-v1.0.pdf) profiles that restrict file, process, and network access when running Claude Code in sandboxed mode.

## Why

Claude Code executes arbitrary shell commands on your machine. Even with permission prompts, mistakes happen. hydra-sandbox generates a macOS sandbox profile that:

- **Blocks reads** to sensitive directories (`.ssh`, `.aws`, `.gnupg`, etc.) and secret file patterns (`.env`, `.tfvars`, private keys)
- **Blocks writes** to personal folders (`Desktop`, `Documents`, `Downloads`, etc.)
- **Restricts process execution** to known safe paths (`/usr`, `/bin`, `/opt/homebrew`, etc.) and the working directory
- **Optionally denies all network access** with `--no-network`

## Requirements

- **macOS** (uses `sandbox-exec` which is macOS-only)
- **[Babashka](https://github.com/babashka/babashka)** v1.0+

Install Babashka:

```sh
brew install borkdude/brew/babashka
```

## Installation

Clone or copy to `~/.config/hydra-sandbox`:

```sh
git clone <repo-url> ~/.config/hydra-sandbox
```

## Configuration

Edit `config.edn` to customize the sandbox rules:

| Key | Description |
|---|---|
| `:sandbox` | SBPL rule templates (header, file-write, process-exec, network-deny) |
| `:deny-read-paths` | Directories to block from reading (use `~/` for home) |
| `:deny-write-paths` | Directories to block from writing |
| `:allow-exec-paths` | Directories where process execution is allowed |
| `:deny-read-patterns` | Regex patterns for files that should not be readable (e.g., `.env`) |
| `:allow-read-patterns` | Exceptions to deny-read-patterns (e.g., `.env.example`) |

Paths that do not exist on the system are automatically skipped.

## Usage

```sh
bb main.bb sandbox-profile <dir> [--no-network]
```

- `<dir>` — the working directory (worktree) to sandbox
- `--no-network` — additionally deny all outbound network and bind

The command writes a temporary `.sb` profile to `/tmp/` and prints its path to stdout. Status messages go to stderr.

### Example

```sh
# Generate a sandbox profile for the current directory
bb main.bb sandbox-profile "$(pwd)"

# Generate with network disabled
bb main.bb sandbox-profile "$(pwd)" --no-network
```

### Integration with Claude Code

Use the generated profile with `sandbox-exec`:

```sh
sandbox-exec -f "$(bb main.bb sandbox-profile "$(pwd)")" claude
```

## Project Structure

```
.
├── config.edn                    # Sandbox rules configuration
├── main.bb                       # CLI entrypoint
├── modules/
│   └── sandbox_profile.bb        # SBPL profile generator
└── bb.edn                        # Babashka project config
```

## How It Works

1. Reads the EDN config defining sandbox rules, paths, and patterns
2. Expands `~/` to the actual home directory and `:worktree` to the target directory
3. Filters out paths that don't exist on the system
4. Assembles SBPL rules as S-expressions (Clojure → SBPL is a natural fit)
5. Writes the profile to a temp file and returns the path

## License

MIT
