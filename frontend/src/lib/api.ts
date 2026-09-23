export type LetterResult = "CORRECT" | "PRESENT" | "ABSENT";
export type GameStatus = "IN_PROGRESS" | "WON" | "LOST";

export type Guess = {
  word: string;
  result: LetterResult[];
};

export type Game = {
  gameId: string;
  guesses: Guess[];
  attemptsRemaining: number;
  status: GameStatus;
  answer?: string;
};

type CreateGameResponse = {
  gameId: string;
  maxAttempts: number;
  status: GameStatus;
};

type SubmitGuessResponse = Guess & {
  attemptsRemaining: number;
  status: GameStatus;
  answer?: string;
};

type ApiError = {
  error: string;
  message: string;
};

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null;
    throw new Error(error?.message ?? "Something went wrong. Please try again.");
  }

  return response.json() as Promise<T>;
}

export async function createGame(): Promise<Game> {
  const created = await request<CreateGameResponse>("/api/games", {
    method: "POST",
  });

  return {
    gameId: created.gameId,
    guesses: [],
    attemptsRemaining: created.maxAttempts,
    status: created.status,
  };
}

export function getGame(gameId: string): Promise<Game> {
  return request<Game>(`/api/games/${gameId}`);
}

export function submitGuess(
  gameId: string,
  word: string,
): Promise<SubmitGuessResponse> {
  return request<SubmitGuessResponse>(`/api/games/${gameId}/guesses`, {
    method: "POST",
    body: JSON.stringify({ word }),
  });
}
