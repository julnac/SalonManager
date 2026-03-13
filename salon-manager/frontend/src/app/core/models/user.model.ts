export interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: string[]; // ['ADMIN'] or ['USER']
}

export interface UserRegistrationDto {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
