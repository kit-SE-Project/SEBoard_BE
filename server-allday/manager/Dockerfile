FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir --require-hashes -r requirements.txt
COPY manager ./manager
RUN useradd --uid 10001 --create-home allday && mkdir /app/.local && chown allday /app/.local
USER 10001
EXPOSE 19090
CMD ["python", "-m", "manager.server", "--config", "/app/.local/config.json"]
