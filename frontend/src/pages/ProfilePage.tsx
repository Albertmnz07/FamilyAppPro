import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useState, type ChangeEvent } from "react";
import { errorMessages, messageForFailure } from "../api/messages";
import { getMe, uploadDniPhotos, type DniPhotoUpload, type DniSide } from "../api/personas";
import { CameraCapture, cameraUnavailableMessage } from "../camera/CameraCapture";

/** The personal menu's copy; exported so tests assert the exact wording. */
export const profileSavedNotice = "Fotos del DNI guardadas.";

const allowedPhotoTypes = ["image/jpeg", "image/png"];
const maxPhotoBytes = 10 * 1024 * 1024;
/** The `accept` attribute restricted to the same types the backend allows. */
const photoAccept = allowedPhotoTypes.join(",");

const dniSides = ["front", "back"] as const;

/** Each side's field label and the multipart part name it is sent under. */
const sideFields: Record<DniSide, { label: string; part: "frontPhoto" | "backPhoto" }> = {
  front: { label: "Foto delantera del DNI", part: "frontPhoto" },
  back: { label: "Foto trasera del DNI", part: "backPhoto" },
};

/** The client-side check that mirrors the backend: JPEG/PNG, at most 10 MB (spec #26). */
function photoProblem(file: File): string | null {
  if (!allowedPhotoTypes.includes(file.type)) return errorMessages["unsupported-photo-type"];
  if (file.size > maxPhotoBytes) return errorMessages["photo-too-large"];
  return null;
}

/** The multipart body that uploads one side on its own. */
function uploadFor(side: DniSide, file: File): DniPhotoUpload {
  return side === "front" ? { frontPhoto: file } : { backPhoto: file };
}

/**
 * The personal menu: the acting Persona's read-only Username and DNI number,
 * plus optional front/back photo upload from a file or the browser camera. There
 * is no edit or logout affordance because the backend offers neither (spec #26).
 */
export function ProfilePage() {
  const queryClient = useQueryClient();
  const [files, setFiles] = useState<Partial<Record<DniSide, File>>>({});
  const [cameraSide, setCameraSide] = useState<DniSide | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const inputRefs = useRef<Record<DniSide, HTMLInputElement | null>>({ front: null, back: null });

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: ({ signal }) => getMe(signal),
  });

  const uploadMutation = useMutation({
    mutationFn: (upload: DniPhotoUpload) => uploadDniPhotos(upload),
    onSuccess: () => {
      setFiles({});
      dniSides.forEach(clearInput);
      setError(null);
      setSaved(true);
      void queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: (failure) => {
      setSaved(false);
      setError(messageForFailure(failure));
    },
  });

  function clearInput(side: DniSide) {
    const input = inputRefs.current[side];
    if (input) input.value = "";
  }

  function dropFile(side: DniSide) {
    setFiles((current) => {
      if (!(side in current)) return current;
      const next = { ...current };
      delete next[side];
      return next;
    });
  }

  function chooseFile(side: DniSide) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      setSaved(false);
      const input = event.currentTarget;
      const file = input.files?.[0];
      if (!file) {
        dropFile(side);
        return;
      }
      const problem = photoProblem(file);
      if (problem) {
        setError(problem);
        clearInput(side);
        dropFile(side);
        return;
      }
      setError(null);
      setFiles((current) => ({ ...current, [side]: file }));
    };
  }

  /** "Aceptar" sends that frame straight away, as the camera criterion asks. */
  function acceptCapture(side: DniSide, file: File) {
    dropFile(side);
    clearInput(side);
    setCameraSide(null);
    setSaved(false);
    setError(null);
    uploadMutation.mutate(uploadFor(side, file));
  }

  function submit() {
    const upload: DniPhotoUpload = {};
    if (files.front) upload.frontPhoto = files.front;
    if (files.back) upload.backPhoto = files.back;
    uploadMutation.mutate(upload);
  }

  const hasFile = Boolean(files.front || files.back);

  return (
    <section className="page">
      <h1>Mi perfil</h1>
      {profileQuery.isPending && (
        <p className="loading" role="status">
          Cargando perfil…
        </p>
      )}
      {profileQuery.isError && (
        <p className="form-error" role="alert">
          {messageForFailure(profileQuery.error)}
          <button type="button" onClick={() => void profileQuery.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      {profileQuery.data && (
        <>
          <dl className="profile-data">
            <dt>Nombre de usuario</dt>
            <dd>{profileQuery.data.username}</dd>
            <dt>DNI</dt>
            <dd>{profileQuery.data.dni}</dd>
          </dl>
          <section className="dni-upload" aria-labelledby="dni-upload-heading">
            <h2 id="dni-upload-heading">Subir fotos del DNI</h2>
            {error && (
              <p className="form-error" role="alert">
                {error}
              </p>
            )}
            {saved && (
              <p className="notice" role="status">
                {profileSavedNotice}
              </p>
            )}
            {dniSides.map((side) => (
              <div className="field" key={side}>
                <label htmlFor={`profile-${side}-photo`}>{sideFields[side].label}</label>
                {cameraSide === side ? (
                  <CameraCapture
                    side={side}
                    onAccept={(file) => acceptCapture(side, file)}
                    onCancel={() => setCameraSide(null)}
                    onUnavailable={() => {
                      setCameraSide(null);
                      setError(cameraUnavailableMessage);
                    }}
                  />
                ) : (
                  <>
                    <input
                      id={`profile-${side}-photo`}
                      name={sideFields[side].part}
                      type="file"
                      accept={photoAccept}
                      ref={(element) => {
                        inputRefs.current[side] = element;
                      }}
                      onChange={chooseFile(side)}
                    />
                    <button type="button" onClick={() => setCameraSide(side)}>
                      Usar cámara
                    </button>
                    {files[side] && (
                      <p className="file-name" role="status">
                        Seleccionada: {files[side]?.name}
                      </p>
                    )}
                  </>
                )}
              </div>
            ))}
            <button
              type="button"
              className="primary"
              onClick={submit}
              disabled={!hasFile || uploadMutation.isPending}
            >
              {uploadMutation.isPending ? "Guardando…" : "Guardar fotos"}
            </button>
          </section>
        </>
      )}
    </section>
  );
}
