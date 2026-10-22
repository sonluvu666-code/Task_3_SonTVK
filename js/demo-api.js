(function () {
  var sessionKey = "smsDemoSession";
  var usersKey = "smsDemoUsers";

  function read(key, fallback) {
    var value = localStorage.getItem(key);
    return value === null ? fallback : JSON.parse(value);
  }

  function write(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function response(status, value) {
    return {
      ok: status >= 200 && status < 300,
      status: status,
      headers: { get: function () { return "application/json"; } },
      json: async function () { return value; }
    };
  }

  function jsonBody(options) {
    if (!options || !options.body || typeof options.body !== "string") return {};
    return JSON.parse(options.body);
  }

  function session() {
    return read(sessionKey, null);
  }

  function historyKey(email) {
    return "smsDemoHistory:" + email.toLowerCase();
  }

  function todosKey(email) {
    return "smsDemoTodos:" + email.toLowerCase();
  }

  function requireSession() {
    var current = session();
    if (!current) throw { status: 401, message: "Please sign in to continue." };
    return current;
  }

  async function handleRequest(path, options) {
    options = options || {};
    var method = (options.method || "GET").toUpperCase();

    if (path === "/api/users" && method === "POST") {
      var registration = jsonBody(options);
      var users = read(usersKey, []);
      registration.email = registration.email.trim().toLowerCase();
      if (users.some(function (user) { return user.email.toLowerCase() === registration.email; })) {
        return response(409, { message: "An account with this email address already exists." });
      }
      if (users.some(function (user) { return user.name === registration.name; })) {
        return response(409, { message: "An account with this name already exists." });
      }
      users.push({ name: registration.name, email: registration.email });
      write(usersKey, users);
      return response(201, { id: users.length, name: registration.name, email: registration.email });
    }

    if (path === "/api/users/login" && method === "POST") {
      var login = jsonBody(options);
      login.email = login.email.trim().toLowerCase();
      var registeredUsers = read(usersKey, []);
      var user = registeredUsers.find(function (candidate) {
        return candidate.email.toLowerCase() === login.email.toLowerCase();
      });
      if (!user) {
        var name = login.email.split("@")[0].replace(/[._-]+/g, " ").trim();
        user = {
          name: name.replace(/\b\w/g, function (letter) { return letter.toUpperCase(); }) || "Guest",
          email: login.email
        };
      }
      write(sessionKey, user);
      var entries = read(historyKey(user.email), []);
      entries.unshift({ id: Date.now(), loginTime: new Date().toISOString(), successful: true });
      write(historyKey(user.email), entries);
      return response(200, { id: 1, name: user.name, email: user.email });
    }

    if (path === "/api/users/me" && method === "GET") {
      var current = session();
      return current
        ? response(200, { id: 1, name: current.name, email: current.email })
        : response(401, { message: "Please sign in to continue." });
    }

    if (path === "/api/users/logout" && method === "POST") {
      localStorage.removeItem(sessionKey);
      return response(204, null);
    }

    if (path === "/api/users/password" && method === "POST") {
      requireSession();
      return response(204, null);
    }

    if (path === "/api/users/profile-image" && method === "POST") {
      var imageSession = requireSession();
      var file = options.body && options.body.get("image");
      if (!file || file.size === 0 || file.size > 5 * 1024 * 1024
          || ["image/png", "image/jpeg", "image/gif"].indexOf(file.type) < 0) {
        return response(400, { message: "Choose a PNG, JPEG, or GIF image no larger than 5 MB." });
      }
      var imageData = await new Promise(function (resolve, reject) {
        var source = new Image();
        var sourceUrl = URL.createObjectURL(file);
        source.onload = function () {
          URL.revokeObjectURL(sourceUrl);
          var scale = Math.min(1, 1024 / Math.max(source.naturalWidth, source.naturalHeight));
          var canvas = document.createElement("canvas");
          canvas.width = Math.max(1, Math.round(source.naturalWidth * scale));
          canvas.height = Math.max(1, Math.round(source.naturalHeight * scale));
          var context = canvas.getContext("2d");
          context.fillStyle = "#ffffff";
          context.fillRect(0, 0, canvas.width, canvas.height);
          context.drawImage(source, 0, 0, canvas.width, canvas.height);
          resolve(canvas.toDataURL("image/jpeg", 0.82));
        };
        source.onerror = function () {
          URL.revokeObjectURL(sourceUrl);
          reject(new Error("The selected photo could not be read."));
        };
        source.src = sourceUrl;
      });
      try {
        write("smsDemoProfileImage:" + imageSession.email.toLowerCase(), imageData);
      } catch (error) {
        return response(400, { message: "The photo could not be saved in this browser. Try a smaller image." });
      }
      return response(204, null);
    }

    if (path === "/api/account/login-history" && method === "GET") {
      var historySession = requireSession();
      return response(200, read(historyKey(historySession.email), []));
    }

    if (path === "/api/account/todos" && method === "GET") {
      var todoSession = requireSession();
      return response(200, read(todosKey(todoSession.email), []));
    }

    if (path === "/api/account/todos" && method === "POST") {
      var addSession = requireSession();
      var todo = jsonBody(options);
      var todos = read(todosKey(addSession.email), []);
      var added = {
        id: Date.now(),
        title: todo.title.trim(),
        dueDate: todo.dueDate || null,
        completed: false
      };
      todos.unshift(added);
      write(todosKey(addSession.email), todos);
      return response(201, added);
    }

    var todoMatch = path.match(/^\/api\/account\/todos\/(\d+)$/);
    if (todoMatch && method === "PATCH") {
      var updateSession = requireSession();
      var updatedTodos = read(todosKey(updateSession.email), []);
      var update = jsonBody(options);
      var item = updatedTodos.find(function (candidate) {
        return candidate.id === Number(todoMatch[1]);
      });
      if (!item) return response(404, { message: "The requested task was not found." });
      item.completed = Boolean(update.completed);
      write(todosKey(updateSession.email), updatedTodos);
      return response(204, null);
    }

    if (todoMatch && method === "DELETE") {
      var deleteSession = requireSession();
      var remainingTodos = read(todosKey(deleteSession.email), []);
      var filteredTodos = remainingTodos.filter(function (candidate) {
        return candidate.id !== Number(todoMatch[1]);
      });
      if (filteredTodos.length === remainingTodos.length) {
        return response(404, { message: "The requested task was not found." });
      }
      write(todosKey(deleteSession.email), filteredTodos);
      return response(204, null);
    }

    return response(404, { message: "This demo feature is unavailable." });
  }

  async function request(path, options) {
    try {
      return await handleRequest(path, options);
    } catch (error) {
      if (error && error.status === 401) {
        return response(401, { message: error.message });
      }
      throw error;
    }
  }

  window.DemoApi = { request: request };
})();
