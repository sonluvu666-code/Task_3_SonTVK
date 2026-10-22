/* Login and registration forms (login.html) - uses Bootstrap validation classes */
(function () {
  var loginPanel = document.getElementById("loginPanel");
  if (!loginPanel) return;

  var loginTab = document.getElementById("loginTab");
  var registerTab = document.getElementById("registerTab");
  var registerPanel = document.getElementById("registerPanel");
  var authTitle = document.getElementById("authTitle");
  var authSubtitle = document.getElementById("authSubtitle");

  function showPanel(panel) {
    var showLogin = panel === loginPanel;
    loginPanel.hidden = !showLogin;
    registerPanel.hidden = showLogin;
    loginTab.classList.toggle("active", showLogin);
    registerTab.classList.toggle("active", !showLogin);
    loginTab.setAttribute("aria-selected", String(showLogin));
    registerTab.setAttribute("aria-selected", String(!showLogin));
    loginTab.tabIndex = showLogin ? 0 : -1;
    registerTab.tabIndex = showLogin ? -1 : 0;
    authTitle.textContent = showLogin ? "Welcome Back!" : "Create your account";
    authSubtitle.textContent = showLogin
      ? "Sign in to continue to your account."
      : "A few details and you're ready to go.";
    panel.querySelector("h2").focus();
  }

  loginTab.addEventListener("click", function () { showPanel(loginPanel); });
  registerTab.addEventListener("click", function () { showPanel(registerPanel); });
  document.getElementById("showRegister").addEventListener("click", function () { showPanel(registerPanel); });
  document.getElementById("showLogin").addEventListener("click", function () { showPanel(loginPanel); });

  var form = document.getElementById("loginForm");
  var email = document.getElementById("email");
  var pass = document.getElementById("password");
  var emailErr = document.getElementById("emailError");
  var passErr = document.getElementById("passError");
  var toggle = document.getElementById("toggle");
  var loginMessage = document.getElementById("loginMessage");

  toggle.addEventListener("click", function () {
    var hidden = pass.type === "password";
    pass.type = hidden ? "text" : "password";
    toggle.textContent = hidden ? "Hide" : "Show";
    toggle.setAttribute("aria-label", hidden ? "Hide password" : "Show password");
  });

  document.getElementById("forgot").addEventListener("click", function (e) {
    e.preventDefault();
    alert("Password reset is not available in this demo.");
  });

  form.addEventListener("submit", function (e) {
    e.preventDefault();
    email.classList.remove("is-invalid");
    pass.classList.remove("is-invalid");
    loginMessage.textContent = "";
    loginMessage.classList.remove("text-danger", "text-success");
    var ok = true;

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
      emailErr.textContent = "Enter a valid email address, like name@example.com.";
      email.classList.add("is-invalid");
      ok = false;
    }
    if (pass.value.length < 6) {
      passErr.textContent = "Password must be at least 6 characters.";
      pass.classList.add("is-invalid");
      ok = false;
    }
    if (!ok) return;

    submitForm(form, "/api/users/login", {
      email: email.value.trim(),
      password: pass.value
    }, loginMessage, async function () {
      window.location.href = "home.html";
    });
  });

  var registerForm = document.getElementById("registerForm");
  var name = document.getElementById("registerName");
  var registerEmail = document.getElementById("registerEmail");
  var registerPassword = document.getElementById("registerPassword");
  var registerMessage = document.getElementById("registerMessage");

  registerForm.addEventListener("submit", function (e) {
    e.preventDefault();
    [name, registerEmail, registerPassword].forEach(function (input) {
      input.classList.remove("is-invalid");
    });
    registerMessage.textContent = "";
    registerMessage.classList.remove("text-danger", "text-success");

    var ok = true;
    if (!name.value.trim() || name.value.trim().length > 100) {
      document.getElementById("nameError").textContent = "Enter a name (up to 100 characters).";
      name.classList.add("is-invalid");
      ok = false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerEmail.value.trim())) {
      document.getElementById("registerEmailError").textContent = "Enter a valid email address, like name@example.com.";
      registerEmail.classList.add("is-invalid");
      ok = false;
    }
    if (registerPassword.value.length < 6 || new TextEncoder().encode(registerPassword.value).length > 72) {
      document.getElementById("registerPasswordError").textContent =
        registerPassword.value.length < 6
          ? "Password must be at least 6 characters."
          : "Password must be no more than 72 bytes.";
      registerPassword.classList.add("is-invalid");
      ok = false;
    }
    if (!ok) return;

    submitForm(registerForm, "/api/users", {
      name: name.value.trim(),
      email: registerEmail.value.trim(),
      password: registerPassword.value
    }, registerMessage, async function () {
      email.value = registerEmail.value.trim();
      pass.value = "";
      registerForm.reset();
      showPanel(loginPanel);
      loginMessage.textContent = "Account created. You can now log in.";
      loginMessage.classList.add("text-success");
    });
  });

  async function submitForm(formElement, endpoint, body, messageElement, onSuccess) {
    var submitButton = formElement.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    try {
      var response;
      try {
        response = await fetch(endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body)
        });
      } catch (error) {
        messageElement.textContent = "Could not connect to the server. Make sure the application is running and try again.";
        messageElement.classList.add("text-danger");
        return;
      }
      var contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) {
        messageElement.textContent = response.status === 404 || response.status === 501
          ? "The account service is not running. Start the Spring Boot app with MySQL configured, then try again."
          : "The account service returned an unexpected response. Please try again.";
        messageElement.classList.add("text-danger");
        return;
      }
      var result;
      try {
        result = await response.json();
      } catch (error) {
        messageElement.textContent = "The account service returned an invalid response. Please try again.";
        messageElement.classList.add("text-danger");
        return;
      }
      if (!response.ok) {
        messageElement.textContent = result.message || "The request could not be completed.";
        messageElement.classList.add("text-danger");
        return;
      }
      await onSuccess(result);
    } finally {
      submitButton.disabled = false;
    }
  }
})();
