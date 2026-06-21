#!/usr/bin/env bash
set -euo pipefail

MODEL="${1:-qwen3.5:2b}"
URL="${OLLAMA_URL:-http://127.0.0.1:11434}"

echo "=== 1. Проверка доступности Ollama ==="
curl -sf "$URL/api/tags" | python3 -c "
import json,sys
data = json.load(sys.stdin)
models = [m['name'] for m in data.get('models', [])]
names = ', '.join(models)
print(f'Доступные модели ({len(models)}): {names}')
target = '$MODEL'
if target not in models:
    print(f'  >>> Модель {target} НЕ найдена!')
    sys.exit(1)
else:
    print(f'  >>> Модель {target} найдена')
"

echo ""
echo "=== 2. Быстрый тест (think off, num_predict=50) ==="
time curl -s --max-time 60 -X POST "$URL/api/chat" \
  -d "{
    \"model\": \"$MODEL\",
    \"messages\": [{\"role\":\"user\",\"content\":\"Say hello in one word\"}],
    \"stream\": false,
    \"options\": {\"num_predict\": 50},
    \"think\": {\"type\": \"disable\"}
  }" | python3 -c "
import json,sys
d=json.load(sys.stdin)
msg = d.get('message', {})
print(f'Ответ: {msg.get(\"content\",\"\")[:200]}')
print(f'Время генерации: {d.get(\"total_duration\",0)/1e9:.1f}s')
"

echo ""
echo "=== 3. Полный тест (как в приложении) ==="
time curl -s --max-time 180 -X POST "$URL/api/chat" \
  -d "{
    \"model\": \"$MODEL\",
    \"messages\": [
      {\"role\":\"user\",\"content\":\"Analyze this alert: CPU usage is 95% on server web-01. Determine priority, category, title, description. Respond in JSON.\"}
    ],
    \"stream\": false
  }" | python3 -c "
import json,sys
d=json.load(sys.stdin)
msg = d.get('message', {})
print(f'Контент: {msg.get(\"content\",\"\")[:300]}')
has_thinking = msg.get('thinking')
if has_thinking:
    print(f'Thinking ({len(has_thinking)} символов)')
print(f'Total tokens: {d.get(\"eval_count\",\"?\")}')
print(f'Общее время: {d.get(\"total_duration\",0)/1e9:.1f}s')
"

echo ""
echo "=== Готово ==="
