/* Login form (login.html) - uses Bootstrap validation classes */
(function () {
  var form = document.getElementById("loginForm");
  if (!form) return;

  var email = document.getElementById("email");
  var pass = document.getElementById("password");
  var emailErr = document.getElementById("emailError");
  var passErr = document.getElementById("passError");
  var toggle = document.getElementById("toggle");

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
  document.getElementById("create").addEventListener("click", function (e) {
    e.preventDefault();
    alert("Account registration is not available in this demo.");
  });

  form.addEventListener("submit", function (e) {
    e.preventDefault();
    email.classList.remove("is-invalid");
    pass.classList.remove("is-invalid");
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

    alert("Login successful");
    window.location.href = "home.html";
  });
})();
