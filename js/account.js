(function () {
  var status = document.querySelector("[data-status]");
  var page = document.body.dataset.page;
  var currentUser;
  var apiBaseUrl = (window.APP_CONFIG && window.APP_CONFIG.apiBaseUrl || "").replace(/\/+$/, "");

  function apiUrl(path) {
    return apiBaseUrl + path;
  }

  document.querySelectorAll("[data-api-path]").forEach(function (link) {
    link.href = apiUrl(link.dataset.apiPath);
  });

  function showStatus(message, isError) {
    status.textContent = message;
    status.classList.toggle("error", Boolean(isError));
    status.classList.toggle("visible", Boolean(message));
  }

  async function api(path, options) {
    var response;
    try {
      response = await fetch(apiUrl(path), Object.assign({ credentials: "include" }, options || {}));
    } catch (error) {
      throw new Error("Could not connect to the server. Check that the application is running.");
    }
    if (response.status === 401) {
      window.location.replace("login.html");
      throw new Error("Your session has expired. Sign in again.");
    }
    if (!response.ok) {
      var problem = await response.json().catch(function () { return {}; });
      throw new Error(problem.message || "The request could not be completed.");
    }
    if (response.status === 204) return null;
    return response.json();
  }

  function jsonOptions(method, data) {
    return {
      method: method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    };
  }

  document.querySelectorAll("[data-logout]").forEach(function (button) {
    button.addEventListener("click", async function () {
      button.disabled = true;
      try {
        await api("/api/users/logout", { method: "POST" });
        window.location.replace("login.html");
      } catch (error) {
        showStatus(error.message, true);
        button.disabled = false;
      }
    });
  });

  async function loadAccount() {
    currentUser = await api("/api/users/me");
    document.querySelectorAll("[data-user-name]").forEach(function (element) {
      element.textContent = currentUser.name;
    });
    document.querySelectorAll("[data-user-email]").forEach(function (element) {
      element.textContent = currentUser.email;
    });
    var initial = document.querySelector("[data-avatar-initial]");
    if (initial) initial.textContent = currentUser.name.trim().charAt(0).toUpperCase();
  }

  async function loadHistory() {
    var entries = await api("/api/account/login-history");
    document.querySelector("[data-history-count]").textContent = entries.length;
    var rows = document.getElementById("historyRows");
    rows.replaceChildren();
    if (entries.length === 0) {
      appendEmptyRow(rows, "No login attempts have been recorded yet.", 4);
      return;
    }
    entries.forEach(function (entry) {
      var row = document.createElement("tr");
      appendCell(row, currentUser.name);
      appendCell(row, currentUser.email);
      appendCell(row, new Date(entry.loginTime).toLocaleString());
      var result = document.createElement("td");
      var badge = document.createElement("span");
      badge.className = "result-badge " + (entry.successful ? "success" : "failed");
      badge.textContent = entry.successful ? "Success" : "Failed";
      result.appendChild(badge);
      row.appendChild(result);
      rows.appendChild(row);
    });
  }

  function appendCell(row, value) {
    var cell = document.createElement("td");
    cell.textContent = value;
    row.appendChild(cell);
  }

  function appendEmptyRow(parent, message, columns) {
    var row = document.createElement("tr");
    var cell = document.createElement("td");
    cell.colSpan = columns;
    cell.className = "empty-state";
    cell.textContent = message;
    row.appendChild(cell);
    parent.appendChild(row);
  }

  async function loadTodos() {
    var todos = await api("/api/account/todos");
    var completed = todos.filter(function (todo) { return todo.completed; }).length;
    document.querySelector("[data-todo-progress]").textContent =
      completed + " of " + todos.length + " tasks completed";
    var list = document.getElementById("todoList");
    list.replaceChildren();
    if (todos.length === 0) {
      var empty = document.createElement("li");
      empty.className = "empty-state";
      empty.textContent = "No tasks yet. Add your first task above.";
      list.appendChild(empty);
      return;
    }
    todos.forEach(function (todo) {
      var item = document.createElement("li");
      item.className = "todo-item" + (todo.completed ? " completed" : "");
      var checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.checked = todo.completed;
      checkbox.setAttribute("aria-label", "Mark " + todo.title + " as completed");
      checkbox.addEventListener("change", function () {
        updateTodo(todo, checkbox.checked);
      });
      var description = document.createElement("div");
      description.className = "todo-description";
      var title = document.createElement("strong");
      title.textContent = todo.title;
      description.appendChild(title);
      if (todo.dueDate) {
        var due = document.createElement("small");
        due.textContent = "Due " + todo.dueDate;
        description.appendChild(due);
      }
      var remove = document.createElement("button");
      remove.type = "button";
      remove.className = "button-secondary delete-task";
      remove.textContent = "Delete";
      remove.addEventListener("click", function () { deleteTodo(todo.id, remove); });
      item.appendChild(checkbox);
      item.appendChild(description);
      item.appendChild(remove);
      list.appendChild(item);
    });
  }

  async function updateTodo(todo, completed) {
    try {
      await api("/api/account/todos/" + todo.id, jsonOptions("PATCH", { completed: completed }));
      await loadTodos();
      showStatus("Task updated.");
    } catch (error) {
      showStatus(error.message, true);
    }
  }

  async function deleteTodo(id, button) {
    button.disabled = true;
    try {
      await api("/api/account/todos/" + id, { method: "DELETE" });
      await loadTodos();
      showStatus("Task deleted.");
    } catch (error) {
      showStatus(error.message, true);
      button.disabled = false;
    }
  }

  function setupProfile() {
    var image = document.querySelector("[data-profile-image]");
    image.addEventListener("load", function () {
      image.hidden = false;
      document.querySelector("[data-avatar-initial]").hidden = true;
    });
    image.addEventListener("error", function () {
      image.hidden = true;
      document.querySelector("[data-avatar-initial]").hidden = false;
    });
    image.src = apiUrl("/api/users/profile-image?refresh=" + Date.now());

    document.getElementById("profileImageFile").addEventListener("change", function (event) {
      var file = event.target.files[0];
      if (file) {
        var preview = URL.createObjectURL(file);
        image.src = preview;
        image.addEventListener("load", function () { URL.revokeObjectURL(preview); }, { once: true });
      }
    });

    document.getElementById("imageForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      var file = document.getElementById("profileImageFile").files[0];
      if (!file) {
        showStatus("Choose an image before uploading.", true);
        return;
      }
      if (file.size > 5 * 1024 * 1024
          || ["image/png", "image/jpeg", "image/gif"].indexOf(file.type) < 0) {
        showStatus("Choose a PNG, JPEG, or GIF image no larger than 5 MB.", true);
        return;
      }
      var data = new FormData();
      data.append("image", file);
      try {
        await api("/api/users/profile-image", { method: "POST", body: data });
        image.src = apiUrl("/api/users/profile-image?updated=" + Date.now());
        document.getElementById("profileImageFile").value = "";
        showStatus("Profile photo uploaded.");
      } catch (error) {
        showStatus(error.message, true);
      }
    });

    document.getElementById("passwordForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      var form = event.currentTarget;
      var currentPassword = document.getElementById("currentPassword");
      var newPassword = document.getElementById("newPassword");
      var confirmPassword = document.getElementById("confirmPassword");
      if (newPassword.value.length < 6) {
        showStatus("Your new password must be at least 6 characters.", true);
        newPassword.focus();
        return;
      }
      if (new TextEncoder().encode(newPassword.value).length > 72) {
        showStatus("Your new password must be no more than 72 bytes.", true);
        newPassword.focus();
        return;
      }
      if (newPassword.value !== confirmPassword.value) {
        showStatus("The new passwords do not match.", true);
        confirmPassword.focus();
        return;
      }
      var submit = form.querySelector('button[type="submit"]');
      submit.disabled = true;
      try {
        await api("/api/users/password", jsonOptions("POST", {
          currentPassword: currentPassword.value,
          newPassword: newPassword.value
        }));
        form.reset();
        showStatus("Password updated successfully.");
      } catch (error) {
        showStatus(error.message, true);
      } finally {
        submit.disabled = false;
      }
    });
  }

  function setupTodos() {
    document.getElementById("todoForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      var form = event.currentTarget;
      var title = document.getElementById("todoTitle");
      var dueDate = document.getElementById("todoDueDate");
      var submit = form.querySelector('button[type="submit"]');
      submit.disabled = true;
      try {
        await api("/api/account/todos", jsonOptions("POST", {
          title: title.value.trim(),
          dueDate: dueDate.value || null
        }));
        form.reset();
        await loadTodos();
        showStatus("Task added.");
      } catch (error) {
        showStatus(error.message, true);
      } finally {
        submit.disabled = false;
      }
    });
  }

  loadAccount().then(function () {
    if (page === "profile") setupProfile();
    if (page === "history") return loadHistory();
    if (page === "todos") {
      setupTodos();
      return loadTodos();
    }
  }).catch(function (error) {
    if (window.location.pathname.indexOf("login.html") < 0) showStatus(error.message, true);
  });
})();
