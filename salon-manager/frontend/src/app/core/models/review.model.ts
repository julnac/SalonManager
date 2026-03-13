export interface ReviewDto {
  id: number;
  content: string;
  createdAt: string; // ISO date-time
  userId: number;
  userName: string;
  imageUrl?: string;
}

export interface CreateReviewRequest {
  content: string; // required, min 10 chars, max 1000 chars
  userId: number;
}
