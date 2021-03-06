package sjj.alog;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;

import sjj.alog.file.LogFile;

/**
 * Created by SJJ on 2017/3/5.
 */

class LogUtils {

    public Config config;
    public LogFile logFile;
    public String lineSeparator = getLineSeparator();
    public ExecutorService executorService;

    LogUtils(Config config) {
        this.config = config;
        executorService = config.getLogExecutorService();
        if (config.writeToFile) {
            logFile = new LogFile(config);
            if (config.deleteOldLogFile)
                logFile.deleteOldLogFile();
        }
    }

    private boolean isEnable(int lev) {
        return config.consolePrintEnable && (config.consolePrintMultiple && lev >= config.consolePrintLev || !config.consolePrintMultiple && config.consolePrintLev == lev);
    }

    void l(final int lev, final Logger.CallMethodException method, final String msg, final Throwable throwable) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String s = throwable == null ? msg : (msg + lineSeparator + throwable(throwable));
                String methodStr = method == null ? "" : method.getMethod();
                writeToFile(lev, methodStr, s);
                if (!isEnable(lev)) return;
                if (config.consolePrintStackTraceLineNum > 0 && throwable != null) {
                    try {
                        BufferedReader reader = new BufferedReader(new StringReader(s));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        int lineCount = 0;
                        while ((line = reader.readLine()) != null && lineCount <= config.consolePrintStackTraceLineNum) {
                            builder.append(line);
                            builder.append(lineSeparator);
                            lineCount++;
                            //第一行是 msg 所以多打印一行
                        }
                        s = builder.toString();
                    } catch (IOException ignored) {
                    }
                }
                int length = s.length();
                if (config.consolePrintAllLog && length > 3500) {
                    for (int i = 0; i < length; i += 3000) {
                        print(lev, methodStr, s.substring(i, Math.min(i + 3000,length)));
                    }
                } else {
                    print(lev, methodStr, s);
                }
            }
        });

    }

    private void print(int lev, String tag, String s) {
        String tag2 = TextUtils.isEmpty(tag) ? "" : (tag + " ");
        switch (lev) {
            case Config.INFO:
                Log.i(config.tag, tag2 + s);
                break;
            case Config.DEBUG:
                Log.d(config.tag, tag2 + s);
                break;
            case Config.WARN:
                Log.w(config.tag, tag2 + s);
                break;
            case Config.ERROR:
                Log.e(config.tag, tag2 + s);
                break;
        }
    }

    private boolean isHoldLog(int lev) {
        return config.writeToFile && (lev >= config.writeToFileLev && config.writeToFileMultiple || !config.writeToFileMultiple && lev == config.writeToFileLev);
    }

    private void writeToFile(int lev, String tag, String msg) {
        if (!isHoldLog(lev)) return;
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        sb.append(hour < 10 ? ("0" + hour) : hour).append(":");
        int min = calendar.get(Calendar.MINUTE);
        sb.append(min < 10 ? ("0" + min) : min).append(":");
        int second = calendar.get(Calendar.SECOND);
        sb.append(second < 10 ? ("0" + second) : second).append(".");
        sb.append(calendar.get(Calendar.MILLISECOND)).append(" ");
        switch (lev) {
            case Config.INFO:
                sb.append("I:");
                break;
            case Config.DEBUG:
                sb.append("D:");
                break;
            case Config.WARN:
                sb.append("W:");
                break;
            case Config.ERROR:
                sb.append("E:");
                break;
        }
        if (logFile != null)
            logFile.push(sb
                    .append(tag)
                    .append(msg)
                    .toString());
    }

    private String getLineSeparator() {
        try {
            String property = System.getProperty("line.separator");
            if (property == null) {
                return "\n";
            }
            return property;
        } catch (Exception e) {
            return "\n";
        }
    }

    private String throwable(Throwable throwable) {
        if (throwable == null) return "";
        if (throwable instanceof UnknownHostException) return "UnknownHostException";
        return Log.getStackTraceString(throwable);
    }
}
