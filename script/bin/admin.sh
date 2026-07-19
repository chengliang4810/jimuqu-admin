#!/bin/sh
# ./admin.sh {start|stop|restart|status}

AppName="${APP_NAME:-jimuqu-admin.jar}"
AppHome=$(pwd)
AppPath="$AppHome/$AppName"
LogPath="$AppHome/logs/$AppName.log"
SolonEnv="${SOLON_ENV:-prod}"
JVM_OPTS="${JVM_OPTS:--Dname=$AppName -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC}"

find_pid() {
    ps -eo pid=,args= | awk -v app="$AppName" '$0 ~ /[j]ava/ && index($0, "-jar") > 0 && index($0, app) > 0 { print $1; exit }'
}

start_app() {
    pid=$(find_pid)
    if [ -n "$pid" ]; then
        printf '%s is already running (pid: %s).\n' "$AppName" "$pid"
        return 0
    fi
    if [ ! -f "$AppPath" ]; then
        printf 'Application JAR does not exist: %s\n' "$AppPath" >&2
        return 1
    fi

    mkdir -p "$(dirname "$LogPath")"
    # JVM_OPTS intentionally supports multiple caller-provided JVM arguments.
    nohup java $JVM_OPTS -jar "$AppPath" "--solon.env=$SolonEnv" >>"$LogPath" 2>&1 &
    pid=$!
    sleep 1
    if ! kill -0 "$pid" 2>/dev/null; then
        printf 'Failed to start %s. Check %s.\n' "$AppName" "$LogPath" >&2
        return 1
    fi
    printf 'Started %s in %s mode (pid: %s).\n' "$AppName" "$SolonEnv" "$pid"
}

stop_app() {
    pid=$(find_pid)
    if [ -z "$pid" ]; then
        printf '%s is already stopped.\n' "$AppName"
        return 0
    fi

    kill -TERM "$pid"
    elapsed=0
    while kill -0 "$pid" 2>/dev/null; do
        if [ "$elapsed" -ge 30 ]; then
            printf 'Timed out waiting for %s (pid: %s) to stop.\n' "$AppName" "$pid" >&2
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    printf 'Stopped %s.\n' "$AppName"
}

status_app() {
    pid=$(find_pid)
    if [ -n "$pid" ]; then
        printf '%s is running (pid: %s).\n' "$AppName" "$pid"
    else
        printf '%s is not running.\n' "$AppName"
        return 1
    fi
}

case "${1:-}" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        stop_app && start_app
        ;;
    status)
        status_app
        ;;
    *)
        printf 'Usage: %s {start|stop|restart|status}\n' "$0" >&2
        exit 1
        ;;
esac
