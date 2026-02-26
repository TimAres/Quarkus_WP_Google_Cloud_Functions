# Optimizing Quarkus Applications with Google Cloud Functions

This project uses [Quarkus](https://quarkus.io/) to implement a **Google Cloud Function** (HTTP) that demonstrates optimization techniques for serverless Java:

- **Quarkus** for fast startup and low footprint (suitable for cold starts).
- **quarkus-google-cloud-functions-http**: REST (JAX-RS) locally and deployable as GCF; token-based auth.
- **Google Cloud Vision API** for document text extraction (e.g. invoice OCR).
- **JVM-first**: Deployment as JVM container on Cloud Run (native build currently not supported due to GraalVM JDK 21 strict image heap).

**Secrets:** Do not commit API keys or passwords. For production, set `google.vision.api.key` and `APP_PASSWORD` via environment variables (e.g. in Cloud Run).

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

The app listens on **http://localhost:8080/** (start page at `/` and `/index.html`, API at `POST /invoice`). You should see a line like `Listening on http://localhost:8080` in the log. The Dev UI is at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Deploy to Google Cloud Run

Use the **JVM** build with the [Quarkus Google Cloud Functions HTTP](https://quarkus.io/guides/gcp-functions-http) extension and deploy the resulting container to **Cloud Run** (Cloud Functions 2nd gen runs on Cloud Run):

1. **Build the app** (default layout; the JVM Dockerfile expects `build/quarkus-app/`):
   ```shell script
   ./gradlew build
   ```

2. **Build the container image** (JVM Dockerfile):
   ```shell script
   docker build -f src/main/docker/Dockerfile.jvm -t gcr.io/YOUR_PROJECT_ID/google-cloud-functions .
   ```

3. **Push and deploy:**
   ```shell script
   docker push gcr.io/YOUR_PROJECT_ID/google-cloud-functions
   gcloud run deploy google-cloud-functions --image gcr.io/YOUR_PROJECT_ID/google-cloud-functions --platform managed --region YOUR_REGION
   ```

Replace `YOUR_PROJECT_ID` and `YOUR_REGION`.

## Related Guides

- Google Cloud Functions HTTP ([guide](https://quarkus.io/guides/gcp-functions-http)): REST deployable as GCF
