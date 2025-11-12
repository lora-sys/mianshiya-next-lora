"use client";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body>
        <h2>发生了一些问题!</h2>
        <button onClick={() => reset()}> 再试一次</button>
      </body>
    </html>
  );
}
