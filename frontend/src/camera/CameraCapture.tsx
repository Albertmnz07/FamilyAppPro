import { useCallback, useEffect, useRef, useState } from "react";
import type { DniSide } from "../api/personas";

/** Shown when the camera cannot be used; the file picker stays available (spec #26). */
export const cameraUnavailableMessage =
  "No se pudo acceder a la cámara. Usa el selector de archivos.";

type Status = "starting" | "live" | "captured";

type Props = {
  /** Names the captured file and its preview. */
  side: DniSide;
  onAccept: (file: File) => void;
  onCancel: () => void;
  /** Called when the camera is missing, insecure or refused; the page falls back to the picker. */
  onUnavailable: () => void;
};

const sideLabel: Record<DniSide, string> = { front: "anverso", back: "reverso" };

/**
 * Captures one DNI photo from the browser camera: live `<video>` preview, then a
 * `<canvas>` frame the user accepts or repeats. The stream is released as soon as
 * the frame is captured, on cancel and on unmount.
 */
export function CameraCapture({ side, onAccept, onCancel, onUnavailable }: Props) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const cancelledRef = useRef(false);
  // Keep the latest callback without making `start` depend on it (which would
  // restart the mount effect on every parent render).
  const onUnavailableRef = useRef(onUnavailable);
  const [status, setStatus] = useState<Status>("starting");
  const [captured, setCaptured] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    onUnavailableRef.current = onUnavailable;
  });

  const stopStream = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }, []);

  const start = useCallback(async () => {
    const media = navigator.mediaDevices;
    if (!media?.getUserMedia || window.isSecureContext === false) {
      onUnavailableRef.current();
      return;
    }
    let stream: MediaStream;
    try {
      stream = await media.getUserMedia({ video: { facingMode: "environment" } });
    } catch {
      onUnavailableRef.current();
      return;
    }
    // The permission prompt can outlive the component: stop the stream rather
    // than letting the camera stay on after the preview is gone.
    if (cancelledRef.current) {
      stream.getTracks().forEach((track) => track.stop());
      return;
    }
    streamRef.current = stream;
    const video = videoRef.current;
    if (video) {
      try {
        video.srcObject = stream;
        await video.play();
      } catch {
        // Autoplay may be refused; the live preview is a nicety, not required.
      }
    }
    if (cancelledRef.current) {
      stopStream();
      return;
    }
    setStatus("live");
  }, [stopStream]);

  useEffect(() => {
    cancelledRef.current = false;
    void start();
    return () => {
      cancelledRef.current = true;
      stopStream();
    };
  }, [start, stopStream]);

  // Release the preview object URL when it is replaced or the component unmounts.
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  function capture() {
    const video = videoRef.current;
    if (!video) return;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const context = canvas.getContext("2d");
    context?.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (!blob) return;
        stopStream();
        setCaptured(new File([blob], `dni-${side}.jpg`, { type: "image/jpeg" }));
        setPreviewUrl(URL.createObjectURL(blob));
        setStatus("captured");
      },
      "image/jpeg",
      0.9,
    );
  }

  function retake() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setCaptured(null);
    setStatus("starting");
    void start();
  }

  return (
    <div className="camera-capture">
      {status === "captured" && previewUrl ? (
        <>
          <img
            className="camera-preview"
            src={previewUrl}
            alt={`Vista previa de la foto capturada (${sideLabel[side]})`}
          />
          <div className="camera-actions">
            <button type="button" onClick={() => captured && onAccept(captured)}>
              Aceptar
            </button>
            <button type="button" onClick={retake}>
              Repetir
            </button>
          </div>
        </>
      ) : (
        <>
          <video ref={videoRef} playsInline muted aria-label="Vista de la cámara" />
          <div className="camera-actions">
            <button type="button" onClick={capture} disabled={status !== "live"}>
              Capturar
            </button>
            <button type="button" onClick={onCancel}>
              Cancelar
            </button>
          </div>
        </>
      )}
    </div>
  );
}
