# AI Receipt Scanner (Quarkus on Google Cloud Functions Gen 2)

## Overview

This project is a serverless AI receipt scanner. It uses Quarkus to implement a Google Cloud Function (HTTP) that processes images of receipts and extracts structured data (Store, Date, Total) using the Google Cloud Vision API.

Visit the Repository under: https://github.com/TimAres/Quarkus_WP_Google_Cloud_Functions.git

Key features of this architecture:

*   **Quarkus:** Provides fast startup times and a low memory footprint, which is ideal for minimizing cold starts in serverless environments.
*   **Cloud Functions Gen 2:** Deployed as a highly optimized JVM artifact running on scalable Google Cloud Run infrastructure.
*   **Google Cloud Vision API:** Utilizes the DOCUMENT_TEXT_DETECTION model for highly robust text extraction, regardless of the receipt's layout or image quality.
*   **Enterprise Security:** Backend authentication with Google Cloud is handled via Identity and Access Management (IAM) using a dedicated Service Account. The web frontend is protected by a custom application password to prevent unauthorized API usage.

## Setup and Deployment

### 1. Prerequisites

Before you begin, ensure you have the following installed and set up:

*   Java 21 (JDK)
*   Google Cloud CLI (gcloud) installed and authenticated.
*   An active Google Cloud Project with billing enabled.

### 2. Google Cloud Preparation

To allow the application to use AI features securely, configure your Google Cloud environment:

1.  **Enable the API:** Navigate to "APIs & Services" in the Google Cloud Console and enable the Cloud Vision API.
2.  **Create a Service Account:** Create a dedicated service account for this function (e.g., `vision-api-caller@<YOUR_PROJECT_ID>.iam.gserviceaccount.com`).
3.  **Assign Roles:** Grant this service account the role **Cloud Vision API User**.

### 3. Local Development (Optional)

To test the application locally, you must provide Application Default Credentials (ADC) so the Google SDK can authenticate:

```bash
# Set the path to your downloaded Service Account JSON key
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/keyfile.json"

# Start Quarkus in Dev Mode
./gradlew quarkusDev
```

The application will be accessible at `http://localhost:8080/`.

### 4. Build and Deploy to Google Cloud

Instead of manually creating Docker containers, we use the gcloud CLI to deploy the Quarkus artifact directly.

**Step 1: Build the application**

The `quarkus-google-cloud-functions-http` extension automatically prepares the required `build/deployment` folder.

```bash
./gradlew clean build
```

**Step 2: Deploy the function**

Replace `<YOUR_PROJECT_ID>` with your actual project ID and set a secure password. The 512MiB memory limit is required for the Vision API image processing.

```bash
gcloud functions deploy quarkus-vision-invoice \
  --gen2 \
  --runtime=java21 \
  --memory=512MiB \
  --trigger-http \
  --allow-unauthenticated \
  --entry-point=io.quarkus.gcp.functions.http.QuarkusHttpFunction \
  --source=build/deployment \
  --region=europe-west3 \
  --service-account=vision-api-caller@<YOUR_PROJECT_ID>.iam.gserviceaccount.com \
  --set-env-vars APP_PASSWORD=Example123
```

## Usage

Once the deployment is successfully completed, the CLI will output a Trigger URL (e.g., `https://europe-west3-...run.app`).

1.  **Open the App:** Open the provided Trigger URL in your web browser.
2.  **Authenticate:** Enter the App Password you defined during deploymentinto the password field.
3.  **Upload an Image:** Click the "Browse..." button to select an image of a receipt (.jpeg or .png). On mobile devices, this will also allow you to take a picture directly using your camera.
4.  **Scan:** Click the "Beleg analysieren" (Analyze Receipt) button.
5.  **View Results:** The image is sent to the backend, processed by the AI, and the extracted data (Store, Date, and Total Amount) will appear on the screen within a few seconds.
