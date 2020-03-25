package dk.itu.postit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Server {
  static final int PORT = 8080;
  static final int BACKLOG = 0; // Use default
  static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final Logger LOG = Logger.getLogger("dk.itu.postit.Server");

  static {
    try {
      FileHandler fh = new FileHandler("logs/" + System.currentTimeMillis() + ".log");
      LOG.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  Map<String, Long> lastAccess = new HashMap<>();
  Map<String, List<String>> boards = new HashMap<>();

  Server() throws IOException {
    final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), BACKLOG);
    server.createContext("/", this::handle);
    server.createContext(
        "/board.js", io -> respond(io, 200, "text/javascript", file("static/board.js")));
    server.createContext(
        "/index.js", io -> respond(io, 200, "text/javascript", file("static/index.js")));
    server.createContext(
        "/client.js", io -> respond(io, 200, "text/javascript", file("static/client.js")));
    server.createContext(
        "/style.css", io -> respond(io, 200, "text/css", file("static/style.css")));
    server.start();
  }

  /*
   * board must match [a-zA-Z0-9]*
   * GET  html /name          student URI for board "name"
   * POST text /name          post a new question to board "name"
   * GET  html /name/view     teacher URI for board "name"
   * GET  json /name/view?f=i teacher URI for board "name"
   */

  Pattern urlPattern =
      Pattern.compile("\\/(?<name>[a-zA-Z0-9]*)(?<view>/view)?(\\?f=(?<from>[0-9]+))?");

  private void handle(HttpExchange io) {
    try {
      var uri = io.getRequestURI().toString();
      Matcher match = urlPattern.matcher(uri);
      if (!match.matches()) {
        respond(io, 404, "text/html", file("static/index.html"));
        return;
      }
      String name = match.group("name");
      String view = match.group("view");
      String from = match.group("from");
      if (name.equals("")) {
        respond(io, 200, "text/html", file("static/index.html"));
      } else {
        List<String> board = boards.get(name);
        if (view == null) {
          if (board == null) {
            respond(io, 404, "text/html", file("static/index.html"));
          } else {
            switch (io.getRequestMethod()) {
              case "GET":
                respond(io, 200, "text/html", file("static/client.html"));
                break;
              case "POST":
                var clientIP = io.getRemoteAddress().getAddress().toString();
                var time = System.currentTimeMillis();
                var last = lastAccess.put(clientIP, time);
                if (last != null && time - last < 1000) {
                  respond(io, 429, "text/plain", "Too many requests");
                  return;
                }
                var available = io.getRequestBody().available();
                if (available <= 2000) {
                  var body = "\"" + escapeHTML(
                      new BufferedReader(new InputStreamReader(io.getRequestBody()))
                          .lines()
                          .collect(Collectors.joining("\\n"))
                  ) + "\"";
                          // .replaceAll("\"", "\\\\\"") + "\"";
                  LOG.info("b=" + name + "&" + body);
                  board.add(body);
                  respond(io, 201, "text/plain", "Post added");
                } else {
                  respond(io, 400, "text/plain", "Post too long");
                }
                break;
              default:
                respond(io, 400, "text/plain", "Invalid request method: " + io.getRequestMethod());
            }
          }
        } else {
          final int code;
          if (board == null) {
            code = 201;
            board = new ArrayList<>();
            boards.put(name, board);
            LOG.info("created " + name);
          } else {
            code = 200;
          }
          if (from == null) {
            respond(io, code, "text/html", file("static/board.html"));
          } else {
            try {
              int f = Math.min(Integer.parseInt(from), board.size());
              var sublist = board.subList(f, board.size());
              if (sublist.isEmpty()) {
                respond(io, 200, "application/json", "[]");
              } else {
                respond(io, 201, "application/json", "[" + String.join(",", sublist) + "]");
              }
            } catch (NumberFormatException e) {
              respond(io, 400, "text/plain", e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      respond(io, 500, "text/plain", "Troels messed up...");
    }
  }

  void respond(HttpExchange io, int code, String mime, String response) {
    try {
      io.getResponseHeaders()
          .set("Content-Type", String.format(mime + "; charset=%s", CHARSET.name()));
      io.sendResponseHeaders(200, response.getBytes(CHARSET.name()).length);
      io.getResponseBody().write(response.getBytes(CHARSET.name()));
    } catch (Exception e) {
    } finally {
      io.close();
    }
  }

  String file(final String path) {
    try {
      return Files.readString(Paths.get(path));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "FILE NOT FOUND";
  }

  static String escapeHTML(String s) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c > 127 || c == '\'' || c == '"' || c == '<' || c == '>' || c == '&') {
            out.append("&#");
            out.append((int) c);
            out.append(';');
        } else {
            out.append(c);
        }
    }
    return out.toString();
  }

  public static void main(final String... args) throws IOException {
    new Server();
  }
}
