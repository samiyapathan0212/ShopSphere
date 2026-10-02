import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { describeApiError, readFieldErrors, useRegisterMutation } from '../app/api/authApi';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_MAX_LENGTH = 72;
const NAME_MAX_LENGTH = 100;

interface RegisterFormState {
  name: string;
  email: string;
  password: string;
}

type RegisterFieldErrors = Partial<Record<keyof RegisterFormState, string>>;

/** Mirrors the backend's RegisterRequest validation rules. */
function validate(form: RegisterFormState): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {};
  const name = form.name.trim();
  const email = form.email.trim();

  if (!name) {
    errors.name = 'Name is required.';
  } else if (name.length > NAME_MAX_LENGTH) {
    errors.name = `Name must be at most ${NAME_MAX_LENGTH} characters.`;
  }

  if (!email) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!form.password) {
    errors.password = 'Password is required.';
  } else if (form.password.length < PASSWORD_MIN_LENGTH) {
    errors.password = `Password must be at least ${PASSWORD_MIN_LENGTH} characters.`;
  } else if (form.password.length > PASSWORD_MAX_LENGTH) {
    errors.password = `Password must be at most ${PASSWORD_MAX_LENGTH} characters.`;
  }

  return errors;
}

/**
 * Registration page (Phase 7). Validates locally, calls
 * `POST /api/auth/register` and — because the backend issues no token on
 * registration — hands the new customer to the sign-in page with a success
 * notice and their email prefilled.
 */
function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterFormState>({ name: '', email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState<RegisterFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [register, { isLoading }] = useRegisterMutation();

  const updateField = (field: keyof RegisterFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const errors = validate(form);
    setFieldErrors(errors);
    setFormError(null);

    if (Object.keys(errors).length > 0) {
      return;
    }

    const email = form.email.trim().toLowerCase();

    try {
      await register({ name: form.name.trim(), email, password: form.password }).unwrap();
      navigate('/login', {
        replace: true,
        state: { notice: 'Account created. Please sign in to continue.', email },
      });
    } catch (error) {
      const apiFieldErrors = readFieldErrors(error);
      const nextFieldErrors: RegisterFieldErrors = {};
      if (apiFieldErrors.name) {
        nextFieldErrors.name = apiFieldErrors.name;
      }
      if (apiFieldErrors.email) {
        nextFieldErrors.email = apiFieldErrors.email;
      }
      if (apiFieldErrors.password) {
        nextFieldErrors.password = apiFieldErrors.password;
      }
      if (Object.keys(nextFieldErrors).length > 0) {
        setFieldErrors(nextFieldErrors);
      }
      setFormError(describeApiError(error, 'Unable to create your account. Please try again.'));
    }
  };

  return (
    <section className="auth-layout">
      <div className="page">
        <p className="eyebrow">Account</p>
        <h1 className="page-title">Create your account</h1>
        <p className="page-lead">
          New customers register here to keep their cart, wishlist and orders together.
        </p>
      </div>

      <div className="auth-card">
        {formError && (
          <p className="form-alert" role="alert">
            {formError}
          </p>
        )}

        <form className="form" onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label className="form-label" htmlFor="register-name">
              Full name
            </label>
            <input
              id="register-name"
              className="form-input"
              type="text"
              name="name"
              autoComplete="name"
              value={form.name}
              onChange={(event) => updateField('name', event.target.value)}
              aria-invalid={Boolean(fieldErrors.name)}
              aria-describedby={fieldErrors.name ? 'register-name-error' : undefined}
              disabled={isLoading}
            />
            {fieldErrors.name && (
              <p className="field-error" id="register-name-error">
                {fieldErrors.name}
              </p>
            )}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="register-email">
              Email
            </label>
            <input
              id="register-email"
              className="form-input"
              type="email"
              name="email"
              autoComplete="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'register-email-error' : undefined}
              disabled={isLoading}
            />
            {fieldErrors.email && (
              <p className="field-error" id="register-email-error">
                {fieldErrors.email}
              </p>
            )}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="register-password">
              Password
            </label>
            <input
              id="register-password"
              className="form-input"
              type="password"
              name="password"
              autoComplete="new-password"
              value={form.password}
              onChange={(event) => updateField('password', event.target.value)}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'register-password-error' : 'register-password-hint'}
              disabled={isLoading}
            />
            {fieldErrors.password ? (
              <p className="field-error" id="register-password-error">
                {fieldErrors.password}
              </p>
            ) : (
              <p className="form-hint" id="register-password-hint">
                Between {PASSWORD_MIN_LENGTH} and {PASSWORD_MAX_LENGTH} characters.
              </p>
            )}
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary btn-lg" disabled={isLoading}>
              {isLoading ? 'Creating account…' : 'Create account'}
            </button>
          </div>
        </form>

        <p className="form-hint">
          Already registered?{' '}
          <Link to="/login" className="text-link">
            Sign in
          </Link>
          .
        </p>
      </div>
    </section>
  );
}

export default RegisterPage;