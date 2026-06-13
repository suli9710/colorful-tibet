FROM python:3.11-slim@sha256:a3ab0b966bc4e91546a033e22093cb840908979487a9fc0e6e38295747e49ac0

WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    HOME=/tmp

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && apt-get install -y --only-upgrade libssl3t64 openssl openssl-provider-legacy \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system app && adduser --system --group app

# Patch base-image Python build tooling flagged by image scanners (not part of
# the hash-locked application requirements).
RUN pip install --no-cache-dir --upgrade "wheel==0.46.2" "jaraco.context==6.1.0"

COPY requirements.txt .
RUN pip install --no-cache-dir --require-hashes -r requirements.txt \
    && scrapling install

COPY . .
RUN chown -R app:app /app /ms-playwright

USER app

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
