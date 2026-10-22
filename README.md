# Management System

The original website files remain in the project root (`index.html`,
`login.html`, `home.html`, `css/`, `js/`, and `images/`). Spring Boot serves
those files in place; they do not need to be moved into a new folder.

The backend uses JDBC and MySQL, not JPA. Java 21 and MySQL Server are required.

## Create the database first

Start MySQL and create the database before starting the application. The
application creates the `users` table but does not create the database:

```sql
CREATE DATABASE demotesting
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Configure a MySQL account with access to `demotesting` and permission to create
tables. Then, in the VS Code PowerShell terminal:

```powershell
$env:DB_USERNAME = "your-mysql-username"
$env:SPRING_DATASOURCE_PASSWORD = "your-local-mysql-password"
Set-Location 'C:\path\to\Task1_SonTVK'
.\mvnw.cmd spring-boot:run
```

This project uses port `8081` so it does not conflict with the other Spring
Boot project already using port `8080`. Open `http://localhost:8081/home.html`
or use the configured VS Code launch entry after starting this project.
`DB_USERNAME` defaults to `root`.
Set `DB_URL` to override the default MySQL URL,
`jdbc:mysql://localhost:3306/demotesting`.

`src/main/resources/schema.sql` creates the account, login-history, profile-image,
and to-do tables on startup. Registration and login API endpoints are
`POST /api/users` and `POST /api/users/login`. Passwords are stored as BCrypt
hashes, and successful sign-ins establish an HTTP session.
New registrations must use a name and email that are not already in use, and
passwords must contain at least 6 characters.

After signing in, the account features are available at `/dashboard.html`,
`/profile.html`, `/login-history.html`, and `/todo.html`. Profile photos accept
PNG, JPEG, or GIF files up to 5 MB. Password changes require the current
password. Login history and tasks are scoped to the signed-in account and can
be downloaded as `.xlsx` files from their respective pages.

For an existing database created before name uniqueness was added, apply this
once after resolving any duplicate names:

```sql
ALTER TABLE users
    ADD CONSTRAINT uk_users_name UNIQUE (name);
```

Run the tests with:

```powershell
.\mvnw.cmd test
```

## Deploy to Railway

The project includes a Dockerfile for deploying the Spring Boot app. Push this
repository to GitHub, create a Railway project from the repository, then add a
Railway MySQL service. In the app service's Variables, add the following
references (replace `MySQL` with the exact name of your Railway database
service):

```text
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

Railway supplies `PORT` for the web service; the application uses it
automatically. After the first successful deploy, open the app service's
Settings and generate a public domain. Share that HTTPS URL with your teacher.
Set `SESSION_COOKIE_SECURE=true` in the app service Variables after enabling
the HTTPS domain. Keep database credentials in Railway Variables; never commit
them to GitHub.
