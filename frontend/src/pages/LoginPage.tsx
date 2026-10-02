import { useEffect, useState, type FormEvent } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { describeApiError, readFieldErrors, useLoginMutation } from '../app/api/authApi';
import { credentialsReceived } from '../app/authSlice';
import type { RootState } from '../app/store';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface LoginFormState {
  email: string;
  password: string;
}

type LoginFieldErrors = Partial<Record<keyof LoginFormState, string>>;

interface LoginLocationState {
  /** Set by the register page after successful account creation. */
  notice?: string;
  /** Prefilled email coming from the register page. */
  email?: string;
  /** Route to return to after a successful sign-in. */
  from?: string;
}

function validate(form: LoginFormState): LoginFieldErrors {
  const errors: LoginFieldErrors = {};
  const email = form.email.trim();

  if (!email) {
    errors.email = 'Email is required.';
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!form.password) {
    errors.password = 'Password is required.';
  }

  return errors;
}

/**
 * Sign-in page (Phase 7). Validates locally, calls `POST /api/auth/login`, shows
 * readable API errors and stores the returned access token in the Redux auth
 * slice before navigating on to the page the customer was heading for.
 */
function LoginPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const location = useLocation();
  const locationState = (location.state ?? null) as LoginLocationState | null;

  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));

  const [form, setForm] = useState<LoginFormState>({
    email: locationState?.email ?? '',
    password: '',
  });
  const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [login, { isLoading }] = useLoginMutation();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/products', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const updateField = (field: keyof LoginFormState, value: string) => {
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

    try {
      const result = await login({
        email: form.email.trim().toLowerCase(),
        password: form.password,
      }).unwrap();

      dispatch(credentialsReceived({ accessToken: result.accessToken, user: result.user }));
      navigate(locationState?.from ?? '/products', { replace: true });
    } catch (error) {
      const apiFieldErrors = readFieldErrors(error);
      const nextFieldErrors: LoginFieldErrors = {};
      if (apiFieldErrors.email) {
        nextFieldErrors.email = apiFieldErrors.email;
      }
      if (apiFieldErrors.password) {
        nextFieldErrors.password = apiFieldErrors.password;
      }
      if (Object.keys(nextFieldErrors).length > 0) {
        setFieldErrors(nextFieldErrors);
      }
      setFormError(describeApiError(error, 'Unable to sign in. Please try again.'));
    }
  };

  return (
    <section className="auth-layout">
      <div className="page">
        <p className="eyebrow">Account</p>
        <h1 className="page-title">Sign in</h1>
        <p className="page-lead">
          Welcome back — sign in to continue to your cart, wishlist and orders.
        </p>
      </div>

      <div className="auth-card">
        {locationState?.notice && (
          <p className="form-success" role="status">
            {locationState.notice}
          </p>
        )}
        {formError && (
          <p className="form-alert" role="alert">
            {formError}
          </p>
        )}

        <form className="form" onSubmit={handleSubmit} noValidate>
          <div className="form-field">
            <label className="form-label" htmlFor="login-email">
              Email
            </label>
            <input
              id="login-email"
              className="form-input"
              type="email"
              name="email"
              autoComplete="email"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'login-email-error' : undefined}
              disabled={isLoading}
            />
            {fieldErrors.email && (
              <p className="field-error" id="login-email-error">
                {fieldErrors.email}
              </p>
            )}
          </div>
        <div className="form-field">
            <label className="form-label" htmlFor="login-password">
              Password
            </label>
            <input
              id="login-password"
              className="form-input"
              type="password"
              name="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(event) => updateField('password', event.target.value)}
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? 'login-password-error' : undefined}
              disabled={isLoading}
            />
            {fieldErrors.password && (
              <p className="field-error" id="login-password-error">
                {fieldErrors.password}
              </p>
            )}
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary btn-lg" disabled={isLoading}>
              {isLoading ? 'Signing in…' : 'Sign in'}
            </button>
          </div>
        </form>

        <p className="form-hint">
          No account yet?{' '}
          <Link to="/register" className="text-link">
            Create one
          </Link>
          .
        </p>
      </div>
    </section>
  );
}

export default LoginPage;