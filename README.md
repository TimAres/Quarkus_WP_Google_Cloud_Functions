# Optimizing Quarkus Applications with Google Cloud Functions (Gen 2)

This project uses [Quarkus](https://quarkus.io/) to implement a **Google Cloud Function** (HTTP) that demonstrates optimization techniques for serverless Java:

- **Quarkus** for fast startup and low footprint (suitable for cold starts in serverless environments).
- **quarkus-google-cloud-functions-http**: Provides a lightweight bridge to run standard REST (JAX-RS) endpoints locally and seamlessly deploy them as a Google Cloud Function.
- **Google Cloud Vision API** for document text extraction (e.g., invoice OCR).
- **Identity & Security (ADC)**: API authentication is handled purely via Google's Application Default Credentials (ADC) by attaching a specific Service Account – no hardcoded API keys needed.
- **JVM-first**: Deployed as a highly optimized JVM artifact on Cloud Functions Gen 2 (which runs on Cloud Run infrastructure under the hood).

**Secrets:** Set `APP_PASSWORD` during deployment as an environment variable to secure the frontend. The Vision API accesses the Cloud via the attached Service Account (e.g., `vision-api-caller` with the role "Cloud Vision API User").

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```bash
./gradlew quarkusDev
```

The app listens on **http://localhost:8080/** (start page at `/` and `/index.html`, API at `POST /invoice`). You should see a line like `Listening on http://localhost:8080` in the log. The Dev UI is at <http://localhost:8080/q/dev/>.

*(Note: If port 8080 is blocked, you can pass `-Dquarkus.http.port=8081` to use a different port).*

## Packaging the application for Google Cloud

The application can be packaged using the standard Gradle build command:

```bash
./gradlew clean build
```

**Important for Deployment:** Because we are using the `quarkus-google-cloud-functions-http` extension, Quarkus does not just build a `.jar`. It automatically generates a specialized `build/deployment` directory. This directory contains the exact structure and dependencies required by the Google Cloud Functions runtime.

## Deploy to Google Cloud Functions (2nd Gen)

Instead of manually building Docker containers, we utilize the official Google Cloud CLI to deploy the prepared Quarkus artifact directly as a 2nd Generation Cloud Function.
Please Note, that you need to set a secret for Google Vision or have other ways to Identify.

Deploy the application using the following command (replace `PROJECT_ID` and the password accordingly):

```bash
gcloud functions deploy quarkus-vision-invoice \
  --gen2 \
  --runtime=java21 \
  --trigger-http \
  --allow-unauthenticated \
  --entry-point=io.quarkus.gcp.functions.http.QuarkusHttpFunction \
  --source=build/deployment \
  --region=europe-west3 \
  --service-account=vision-api-caller@PROJECT_ID.iam.gserviceaccount.com \
  --set-env-vars APP_PASSWORD=dein-geheimes-passwort
```

**What happens here:**
1. `--source=build/deployment`: Uploads the optimized Quarkus build.
2. `--entry-point=...QuarkusHttpFunction`: Uses the Quarkus-provided bridge to route incoming HTTP requests to your JAX-RS endpoints.
3. `--service-account`: Attaches the identity to the function so the Vision API SDK can authenticate automatically via ADC.

## Related Guides

- Google Cloud Functions HTTP ([guide](https://quarkus.io/guides/gcp-functions-http)): REST deployable as GCF