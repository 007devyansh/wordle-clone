"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createGame,
  getGame,
  submitGuess,
  type Game,
  type LetterResult,
} from "@/lib/api";

const GAME_ID_KEY = "wordle-game-id";
const MAX_ATTEMPTS = 6;
const WORD_LENGTH = 5;
const keyboardRows = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];

const tileStyles: Record<LetterResult, string> = {
  CORRECT: "border-emerald-500 bg-emerald-500 text-white",
  PRESENT: "border-amber-400 bg-amber-400 text-slate-950",
  ABSENT: "border-slate-700 bg-slate-700 text-white",
};

const keyStyles: Record<LetterResult, string> = {
  CORRECT: "bg-emerald-500 text-white hover:bg-emerald-400",
  PRESENT: "bg-amber-400 text-slate-950 hover:bg-amber-300",
  ABSENT: "bg-slate-900 text-slate-500 hover:bg-slate-800",
};

const resultPriority: Record<LetterResult, number> = {
  ABSENT: 0,
  PRESENT: 1,
  CORRECT: 2,
};

export default function Home() {
  const [game, setGame] = useState<Game | null>(null);
  const [input, setInput] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [shakeCount, setShakeCount] = useState(0);

  const startNewGame = useCallback(async () => {
    setIsLoading(true);
    setError("");
    setInput("");

    try {
      const newGame = await createGame();
      localStorage.setItem(GAME_ID_KEY, newGame.gameId);
      setGame(newGame);
    } catch (caughtError) {
      setError(messageFrom(caughtError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const restoreGame = async () => {
      const savedGameId = localStorage.getItem(GAME_ID_KEY);

      if (!savedGameId) {
        await startNewGame();
        return;
      }

      try {
        setGame(await getGame(savedGameId));
      } catch {
        localStorage.removeItem(GAME_ID_KEY);
        await startNewGame();
        return;
      } finally {
        setIsLoading(false);
      }
    };

    void restoreGame();
  }, [startNewGame]);

  const addLetter = useCallback(
    (letter: string) => {
      if (!game || game.status !== "IN_PROGRESS" || isSubmitting) return;
      setError("");
      setInput((current) =>
        current.length < WORD_LENGTH ? `${current}${letter}` : current,
      );
    },
    [game, isSubmitting],
  );

  const removeLetter = useCallback(() => {
    if (!game || game.status !== "IN_PROGRESS" || isSubmitting) return;
    setInput((current) => current.slice(0, -1));
  }, [game, isSubmitting]);

  const submitCurrentGuess = useCallback(async () => {
    if (!game || game.status !== "IN_PROGRESS" || isSubmitting) return;

    if (input.length !== WORD_LENGTH) {
      setShakeCount((current) => current + 1);
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const response = await submitGuess(game.gameId, input);
      setGame((currentGame) =>
        currentGame
          ? {
              ...currentGame,
              guesses: [
                ...currentGame.guesses,
                { word: response.word, result: response.result },
              ],
              attemptsRemaining: response.attemptsRemaining,
              status: response.status,
              answer: response.answer,
            }
          : currentGame,
      );
      setInput("");
    } catch (caughtError) {
      setError(messageFrom(caughtError));
    } finally {
      setIsSubmitting(false);
    }
  }, [game, input, isSubmitting]);

  const submittedGuesses = useMemo(() => game?.guesses ?? [], [game]);
  const isFinished = game?.status === "WON" || game?.status === "LOST";
  const keyboardStates = useMemo(() => {
    const states: Partial<Record<string, LetterResult>> = {};

    submittedGuesses.forEach((guess) => {
      guess.word.split("").forEach((letter, index) => {
        const nextResult = guess.result[index];
        const currentResult = states[letter];

        if (
          nextResult &&
          (!currentResult || resultPriority[nextResult] > resultPriority[currentResult])
        ) {
          states[letter] = nextResult;
        }
      });
    });

    return states;
  }, [submittedGuesses]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        event.preventDefault();
        if (isFinished) {
          void startNewGame();
        } else {
          void submitCurrentGuess();
        }
      } else if (event.key === "Backspace") {
        removeLetter();
      } else if (/^[a-zA-Z]$/.test(event.key)) {
        addLetter(event.key.toUpperCase());
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [addLetter, isFinished, removeLetter, startNewGame, submitCurrentGuess]);

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-6 text-slate-100 sm:px-6">
      <div className="mx-auto flex min-h-[calc(100vh-3rem)] max-w-md flex-col">
        <header className="flex items-center justify-between border-b border-slate-800 pb-5">
          <div>
            <p className="text-xs font-semibold tracking-[0.28em] text-emerald-400">
              UNLIMITED MODE
            </p>
            <h1 className="mt-1 text-3xl font-black tracking-[0.18em]">WRDL</h1>
          </div>
          <button
            type="button"
            onClick={() => void startNewGame()}
            disabled={isLoading || isSubmitting}
            className="rounded-lg border border-slate-700 px-3 py-2 text-xs font-bold uppercase tracking-wider transition hover:border-emerald-400 hover:text-emerald-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            New game
          </button>
        </header>

        <section className="flex flex-1 flex-col justify-center py-8">
          {isLoading ? (
            <p className="text-center text-sm text-slate-400">Loading game…</p>
          ) : (
            <>
              <div className="mx-auto w-full max-w-[350px] space-y-1.5 sm:space-y-2">
                {Array.from({ length: MAX_ATTEMPTS }, (_, row) => {
                  const guess = submittedGuesses[row];
                  const isCurrentRow = row === submittedGuesses.length && !isFinished;

                  return (
                    <div
                      key={isCurrentRow ? `active-row-${shakeCount}` : row}
                      className={`grid grid-cols-5 gap-1.5 sm:gap-2 ${
                        isCurrentRow && shakeCount > 0
                          ? "animate-[wordle-shake_0.35s_ease-in-out]"
                          : ""
                      }`}
                    >
                      {Array.from({ length: WORD_LENGTH }, (_, column) => {
                        const letter = guess?.word[column] ?? (isCurrentRow ? input[column] : "");
                        const result = guess?.result[column];

                        return (
                          <div
                            key={column}
                            className={`flex aspect-square items-center justify-center rounded-md border-2 text-2xl font-black uppercase transition sm:text-3xl ${
                              result
                                ? tileStyles[result]
                                : letter
                                  ? "border-slate-500 bg-slate-900"
                                  : "border-slate-800 bg-slate-900/50"
                            }`}
                          >
                            {letter}
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>

              <p aria-live="polite" className="mt-5 min-h-5 text-center text-sm text-rose-300">
                {error}
              </p>
            </>
          )}
        </section>

        <section aria-label="Keyboard" className="space-y-2 pb-2">
          {keyboardRows.map((row, rowIndex) => (
            <div key={row} className="flex justify-center gap-1.5">
              {rowIndex === 2 && <KeyboardButton label="Enter" onClick={() => void submitCurrentGuess()} wide />}
              {[...row].map((letter) => (
                <KeyboardButton
                  key={letter}
                  label={letter}
                  onClick={() => addLetter(letter)}
                  result={keyboardStates[letter]}
                />
              ))}
              {rowIndex === 2 && <KeyboardButton label="⌫" onClick={removeLetter} wide />}
            </div>
          ))}
        </section>
      </div>

      {isFinished && game && (
        <div className="fixed inset-0 flex items-center justify-center bg-slate-950/80 px-4 backdrop-blur-sm">
          <section className="w-full max-w-sm rounded-2xl border border-slate-700 bg-slate-900 p-7 text-center shadow-2xl">
            <p className="text-sm font-bold uppercase tracking-[0.22em] text-emerald-400">
              {game.status === "WON" ? "Excellent" : "Game over"}
            </p>
            <h2 className="mt-2 text-3xl font-black">
              {game.status === "WON" ? "You won!" : "You lost"}
            </h2>
            {game.status === "LOST" && game.answer && (
              <p className="mt-4 text-slate-300">
                The word was <span className="font-black tracking-widest text-white">{game.answer}</span>
              </p>
            )}
            <button
              type="button"
              onClick={() => void startNewGame()}
              className="mt-7 w-full rounded-lg bg-emerald-500 px-4 py-3 text-sm font-black uppercase tracking-wider text-slate-950 transition hover:bg-emerald-400"
            >
              Play again
            </button>
          </section>
        </div>
      )}
    </main>
  );
}

function KeyboardButton({
  label,
  onClick,
  result,
  wide = false,
}: {
  label: string;
  onClick: () => void;
  result?: LetterResult;
  wide?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex h-12 items-center justify-center rounded-md px-1 text-xs font-black uppercase transition active:scale-95 sm:h-14 sm:text-sm ${
        result ? keyStyles[result] : "bg-slate-700 hover:bg-slate-600"
      } ${
        wide ? "w-12 sm:w-16" : "w-7 sm:w-9"
      }`}
    >
      {label}
    </button>
  );
}

function messageFrom(error: unknown) {
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}
