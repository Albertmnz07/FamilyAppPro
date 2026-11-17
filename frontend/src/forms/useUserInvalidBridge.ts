import { useEffect, useRef } from "react";

const FIELD_SELECTOR = "input, select, textarea";

type Field = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

/**
 * The accessible error bridge (spec #14, "Forms"): keeps `aria-invalid` in sync
 * with the visual error state — CSS `:user-invalid` in real browsers — using
 * the interaction model of the accessible-error-announcement guide: a field
 * counts as invalid once the user has edited it and left it, or when a submit
 * attempt fires its `invalid` event. `aria-invalid="false"` is set from the
 * start rather than left unset, the field's error message is listed in
 * `aria-describedby` only while it applies, and one polite live region per page
 * announces an error that appears when the user leaves a field. The state is
 * derived from events instead of `matches(':user-invalid')` so it also works
 * in jsdom, which never matches that selector.
 */
export function useUserInvalidBridge(): React.RefObject<HTMLFormElement | null> {
  const formRef = useRef<HTMLFormElement | null>(null);

  useEffect(() => {
    const form = formRef.current;
    if (!form) return;

    const announcer = document.createElement("div");
    announcer.setAttribute("aria-live", "polite");
    announcer.className = "visually-hidden";
    document.body.append(announcer);

    const interacted = new WeakSet<Element>();
    const left = new WeakSet<Element>();
    const flagged = new WeakSet<Element>();
    const announced = new WeakSet<Element>();
    let announceTimer: ReturnType<typeof setTimeout> | undefined;
    let announcedField: Element | undefined;

    // Text as a screen reader reads it: icons' text alternatives in, aria-hidden
    // text out.
    const spokenText = (node: Element): string => {
      const copy = node.cloneNode(true) as Element;
      copy.querySelectorAll('[aria-hidden="true"]').forEach((hidden) => hidden.remove());
      copy.querySelectorAll('[role="img"][aria-label]').forEach((image) =>
        image.replaceWith(`${image.getAttribute("aria-label")}: `),
      );
      return (copy.textContent ?? "").replace(/\s+/g, " ").trim();
    };

    const announceError = (field: Field): void => {
      const errorId = field.getAttribute("data-error");
      const errorElement = errorId ? document.getElementById(errorId) : null;
      if (!errorElement) return;
      const label = field.labels?.[0];
      const prefix = label ? `${spokenText(label)}: ` : "";
      clearTimeout(announceTimer);
      announcer.textContent = "";
      announcedField = field;
      // A moment later, so the announcement doesn't talk over the field that
      // received focus.
      announceTimer = setTimeout(() => {
        announcer.textContent = `${prefix}${spokenText(errorElement)}`;
      }, 500);
    };

    // List the field's error message (the id in data-error) in aria-describedby
    // only while it is shown: text that aria-describedby references is announced
    // even when it's hidden.
    const linkError = (field: Field, shown: boolean): void => {
      const errorId = field.getAttribute("data-error");
      if (!errorId) return;
      const describedBy = (field.getAttribute("aria-describedby") ?? "")
        .split(/\s+/)
        .filter((id) => id && id !== errorId);
      if (shown) describedBy.push(errorId);
      if (describedBy.length) field.setAttribute("aria-describedby", describedBy.join(" "));
      else field.removeAttribute("aria-describedby");
    };

    const applyState = (field: Field, invalid: boolean, announce: boolean): void => {
      if (invalid) {
        if (announce && !announced.has(field)) {
          announceError(field);
          announced.add(field);
        }
        field.setAttribute("aria-invalid", "true");
        linkError(field, true);
        flagged.add(field);
      } else {
        field.setAttribute("aria-invalid", "false");
        linkError(field, false);
        flagged.delete(field);
        announced.delete(field);
        if (announcedField === field) {
          clearTimeout(announceTimer);
          announcer.textContent = "";
          announcedField = undefined;
        }
      }
    };

    const isInvalid = (field: Field): boolean =>
      field.willValidate && !field.validity.valid && interacted.has(field) && left.has(field);

    const refresh = (field: Element, announce = false): void => {
      if (!field.matches(FIELD_SELECTOR)) return;
      const el = field as Field;
      if (!el.willValidate) return;
      applyState(el, isInvalid(el), announce);
    };

    const onInput = (event: Event): void => {
      const field = event.target as Element;
      if (!field.matches(FIELD_SELECTOR)) return;
      interacted.add(field);
      // Clear an already-flagged field as soon as its value becomes valid, but
      // never introduce errors while the user is typing.
      if (flagged.has(field)) refresh(field);
    };

    const onBlur = (event: FocusEvent): void => {
      const field = event.target as Element;
      if (!field.matches(FIELD_SELECTOR)) return;
      left.add(field);
      if (interacted.has(field)) refresh(field, true);
    };

    const onChange = (event: Event): void => {
      const field = event.target as Element;
      if (!field.matches(FIELD_SELECTOR)) return;
      interacted.add(field);
      refresh(field, true);
    };

    const onInvalid = (event: Event): void => {
      // A failed submit fires `invalid` on every failing field, including ones
      // the user never touched. The browser moves focus to the first invalid
      // field, whose error is then announced by focus instead of the live region.
      const field = event.target as Element;
      if (!field.matches(FIELD_SELECTOR)) return;
      interacted.add(field);
      left.add(field);
      announced.add(field);
      const el = field as Field;
      if (el.willValidate) applyState(el, true, false);
    };

    // Start every field at aria-invalid="false".
    form.querySelectorAll(FIELD_SELECTOR).forEach((field) => refresh(field));

    form.addEventListener("blur", onBlur, true);
    form.addEventListener("input", onInput);
    form.addEventListener("change", onChange, true);
    form.addEventListener("invalid", onInvalid, true);

    return () => {
      clearTimeout(announceTimer);
      form.removeEventListener("blur", onBlur, true);
      form.removeEventListener("input", onInput);
      form.removeEventListener("change", onChange, true);
      form.removeEventListener("invalid", onInvalid, true);
      announcer.remove();
    };
  }, []);

  return formRef;
}
