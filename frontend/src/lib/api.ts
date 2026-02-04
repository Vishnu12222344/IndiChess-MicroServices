/**
 * api.ts — Frontend API layer
 *
 * CHANGE FROM MONOLITH:
 *   The frontend now talks exclusively to the API Gateway (port 8080).
 *   The gateway routes /auth/** → User Service and /match/** → Match Service.
 *   No other change is needed in the frontend; it doesn't know or care
 *   about the individual service ports.
 *
 * The Match type is updated to use flat email strings (player1Email /
 * player2Email) to match what MatchDTO sends back from Match Service.
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'; // ← API Gateway

/* =======================
   Types
======================= */

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface RegisterData {
    name: string;
    email: string;
    password: string;
}

export interface User {
    id: number;
    name: string;
    email: string;
}

/* =======================
   MATCH TYPES  (updated to match MatchDTO from Match Service)
======================= */

export interface Match {
    id: number;
    status: 'WHITE_WIN' | 'BLACK_WIN' | 'DRAW' | 'ONGOING';

    // Flat email strings — no nested User objects
    player1Email?: string;
    player2Email?: string;

    fenCurrent: string;
    currentTurnEmail: string;
    lastMoveUci?: string;
    currentPly: number;
    gameType: 'BLITZ' | 'RAPID';
    whiteTime: number;   // seconds
    blackTime: number;   // seconds
    lastMoveTime: number; // epoch ms
}

/* =======================
   Error class
======================= */

export class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
        this.name = 'ApiError';
    }
}

/* =======================
   Helpers
======================= */

const getAuthHeaders = (): HeadersInit => {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
    };
};

/* =======================
   AUTH API  (routed to User Service via gateway)
======================= */

export const authApi = {
    async login(credentials: LoginCredentials): Promise<string> {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials),
            credentials: 'include',
        });

        if (!response.ok) {
            if (response.status === 401) throw new ApiError(401, 'Invalid email or password');
            throw new ApiError(response.status, 'Login failed');
        }

        const data = await response.json();
        const token = typeof data === 'string' ? data : data.token;
        if (!token) throw new ApiError(500, 'Empty token received');
        return token;
    },

    async register(data: RegisterData): Promise<User> {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
            credentials: 'include',
        });

        if (!response.ok) {
            if (response.status === 409) throw new ApiError(409, 'Email already registered');
            throw new ApiError(response.status, 'Registration failed');
        }

        return response.json();
    },
};

/* =======================
   MATCH API  (routed to Match Service via gateway)
======================= */

export const matchApi = {
    async createPrivateMatch(): Promise<Match> {
        const response = await fetch(`${API_BASE_URL}/match/create-private`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'include',
        });

        if (!response.ok) throw new ApiError(response.status, 'Failed to create match');
        return response.json();
    },

    async joinMatch(matchId: number): Promise<Match> {
        const response = await fetch(`${API_BASE_URL}/match/${matchId}/join`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'include',
        });

        if (!response.ok) throw new ApiError(response.status, 'Failed to join match');
        return response.json();
    },

    async getMatch(id: number): Promise<Match> {
        const response = await fetch(`${API_BASE_URL}/match/${id}`, {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'include',
        });

        if (!response.ok) throw new ApiError(response.status, 'Failed to fetch match');
        return response.json();
    },

    async resign(matchId: number): Promise<Match> {
        const response = await fetch(`${API_BASE_URL}/match/${matchId}/resign`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'include',
        });

        if (!response.ok) {
            const error = await response.text();
            throw new ApiError(response.status, error || 'Resignation failed');
        }
        return response.json();
    },
};