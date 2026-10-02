import { useRef, useState, type FormEvent } from 'react';
import { useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { describeApiError, readFieldErrors } from '../app/api/authApi';
import { useGetCartQuery } from '../app/api/cartApi';
import { useCheckoutMutation } from '../app/api/checkoutApi';
import type { RootState } from '../app/store';
import { formatPrice } from '../mocks/catalog';

interface CheckoutFormState {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

type CheckoutFieldErrors = Partial<Record<keyof CheckoutFormState, string>>;

/** Fields the backend marks `@NotBlank` in `CheckoutRequest`. */
const REQUIRED_FIELDS: Array<{ field: keyof CheckoutFormState; label: string }> = [
  { field: 'recipientName', label: 'Recipient name' },
  { field: 'phone', label: 'Phone' },
  { field: 'addressLine1', label: 'Address line 1' },
  { field: 'city', label: 'City' },
  { field: 'state', label: 'State or region' },
  { field: 'postalCode', label: 'Postal code' },
  { field: 'country', label: 'Country' },
];

function validate(form: CheckoutFormState): CheckoutFieldErrors {
  const errors: CheckoutFieldErrors = {};
  for (const { field, label } of REQUIRED_FIELDS) {
    if (!form[field].trim()) {
      errors[field] = `${label} is required.`;
    }
  }
  return errors;
}

const TEXT_FIELDS: Array<{ field: keyof CheckoutFormState; label: string; autoComplete: string }> = [
  { field: 'recipientName', label: 'Recipient name', autoComplete: 'name' },
  { field: 'phone', label: 'Phone', autoComplete: 'tel' },
  { field: 'addressLine1', label: 'Address line 1', autoComplete: 'address-line1' },
  { field: 'addressLine2', label: 'Address line 2 (optional)', autoComplete: 'address-line2' },
  { field: 'city', label: 'City', autoComplete: 'address-level2' },
  { field: 'state', label: 'State or region', autoComplete: 'address-level1' },
  { field: 'postalCode', label: 'Postal code', autoComplete: 'postal-code' },
  { field: 'country', label: 'Country', autoComplete: 'country-name' },
];

/**
 * Checkout page (Phase 5 client). Collects the shipping address that the
 * backend snapshots onto the order and submits the single `POST /api/checkout`
 * transaction, which creates the order, charges the sandbox payment and clears
 * the cart server-side. On success the customer lands on the confirmation page
 * for the returned order.
 *
 * There is no separate card form: payment is part of the checkout transaction,
 * so the resulting payment state is displayed rather than collected.
 */
function CheckoutPage() {
  const navigate = useNavigate();
  const user = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = useSelector((state: RootState) => Boolean(state.auth.accessToken));

  const cartQuery = useGetCartQuery(undefined, { skip: !isAuthenticated });
  const [checkout, { isLoading }] = useCheckoutMutation();

  const [form, setForm] = useState<CheckoutFormState>(() => ({
    recipientName: user?.name ?? '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
  }));
  const [fieldErrors, setFieldErrors] = useState<CheckoutFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  // A ref (not state) guards the submit: state updates are asynchronous, so a
  // fast double click could otherwise fire two checkouts before the button
  // re-renders as disabled. The backend has no idempotency key, so a second
  // checkout would create and charge a second order.
  const submitting = useRef(false);

  const updateField = (field: keyof CheckoutFormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting.current) {
      return;
    }

    const errors = validate(form);
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) {
      return;
    }

    submitting.current = true;
    try {
      const result = await checkout({
        recipientName: form.recipientName.trim(),
        phone: form.phone.trim(),
        addressLine1: form.addressLine1.trim(),
        addressLine2: form.addressLine2.trim() ? form.addressLine2.trim() : null,
        city: form.city.trim(),
        state: form.state.trim(),
        postalCode: form.postalCode.trim(),
        country: form.country.trim(),
        simulatePaymentFailure: false,
      }).unwrap();

      // The checkout result travels in router state so the confirmation page can
      // render the order and its payment immediately; that page also refetches by
      // id, so a reload or a shared link still works.
      navigate(`/order-confirmation/${result.order.id}`, {
        state: { checkout: result },
        replace: true,
      });
    } catch (error) {
      const apiFieldErrors = readFieldErrors(error);
      const nextFieldErrors: CheckoutFieldErrors = {};
      for (const { field } of REQUIRED_FIELDS) {
        const message = apiFieldErrors[field];
        if (message) {
          nextFieldErrors[field] = message;
        }
      }
      if (Object.keys(nextFieldErrors).length > 0) {
        setFieldErrors(nextFieldErrors);
      }
      setFormError(
        describeApiError(error, 'We could not complete your checkout. Please try again.'),
      );
    } finally {
      submitting.current = false;
    }
  };

  const cart = cartQuery.data;
  const busy = isLoading;

  return (
    <section className="page-section">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link to="/cart">Cart</Link>
        <span aria-hidden="true">/</span>
        <span>Checkout</span>
      </nav>

      <header className="page-header">
        <div>
          <h1 className="page-title">Checkout</h1>
          <p className="page-lead">
            Confirm where your order should go. Payment is taken as part of placing the order, and
            the result is shown on the confirmation screen.
          </p>
        </div>
      </header>

      {!isAuthenticated ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Sign in to check out</h2>
          <p className="empty-state-text">
            Checkout belongs to your customer account, so sign in to place this order.
          </p>
          <div className="empty-state-actions">
            <Link to="/login" className="btn btn-primary">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-outline">
              Create an account
            </Link>
          </div>
        </div>
      ) : cartQuery.isLoading ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">Loading your cart…</h2>
          <p className="empty-state-text">Checking the items you are about to order.</p>
        </div>
      ) : cartQuery.isError ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">We could not load your cart</h2>
          <p className="empty-state-text">
            {describeApiError(cartQuery.error, 'The cart service is not responding right now.')}
          </p>
          <div className="empty-state-actions">
            <button type="button" className="btn btn-primary" onClick={() => cartQuery.refetch()}>
              Try again
            </button>
            <Link to="/cart" className="btn btn-outline">
              Back to cart
            </Link>
          </div>
        </div>
      ) : !cart || cart.items.length === 0 ? (
        <div className="empty-state-card empty-state-card--centered">
          <h2 className="empty-state-title">There is nothing to check out</h2>
          <p className="empty-state-text">
            Your cart is empty. Add a product to your cart and we will bring you back here.
          </p>
          <div className="empty-state-actions">
            <Link to="/products" className="btn btn-primary">
              Start shopping
            </Link>
            <Link to="/cart" className="btn btn-outline">
              View cart
            </Link>
          </div>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="cart-main">
            {formError && (
              <p className="form-alert" role="alert">
                {formError}
              </p>
            )}

            <div className="order-panel">
              <h2 className="order-panel-title">Delivery address</h2>
              <form className="form" onSubmit={handleSubmit} noValidate>
                {TEXT_FIELDS.map(({ field, label, autoComplete }) => (
                  <div className="form-field" key={field}>
                    <label className="form-label" htmlFor={`checkout-${field}`}>
                      {label}
                    </label>
                    <input
                      id={`checkout-${field}`}
                      className="form-input"
                      type="text"
                      name={field}
                      autoComplete={autoComplete}
                      value={form[field]}
                      onChange={(event) => updateField(field, event.target.value)}
                      aria-invalid={Boolean(fieldErrors[field])}
                      aria-describedby={fieldErrors[field] ? `checkout-${field}-error` : undefined}
                      disabled={busy}
                    />
                    {fieldErrors[field] && (
                      <p className="field-error" id={`checkout-${field}-error`}>
                        {fieldErrors[field]}
                      </p>
                    )}
                  </div>
                ))}

                <div className="form-actions">
                  <button type="submit" className="btn btn-primary btn-lg" disabled={busy}>
                    {busy ? 'Placing your order…' : 'Place order'}
                  </button>
                  <p className="form-hint">
                    Your order is placed and paid in a single step. The payment result appears on the
                    confirmation screen.
                  </p>
                </div>
              </form>
            </div>
          </div>

          <aside className="cart-summary" aria-label="Order summary">
            <h2 className="cart-summary-title">Your order</h2>

            <ul className="order-summary-items">
              {cart.items.map((item) => (
                <li key={item.productId}>
                  <span>
                    {item.productName} &times; {item.quantity}
                  </span>
                  <strong>{formatPrice(item.subtotal)}</strong>
                </li>
              ))}
            </ul>

            <div className="cart-summary-row">
              <span>Items</span>
              <strong>{cart.itemCountWithQuantity}</strong>
            </div>
            <div className="cart-summary-row cart-summary-total">
              <span>Subtotal</span>
              <strong>{formatPrice(cart.subtotal)}</strong>
            </div>
            <p className="cart-summary-note">
              Delivery is calculated when your order is placed; the final total is shown on the
              confirmation screen.
            </p>
            <Link to="/cart" className="btn btn-outline btn-block">
              Back to cart
            </Link>
          </aside>
        </div>
      )}
    </section>
  );
}

export default CheckoutPage;
