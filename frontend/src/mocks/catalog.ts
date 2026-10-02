/**
 * Mock catalog data — UI demonstration only.
 *
 * This module is the single source of storefront demo data. It is deliberately
 * isolated from the REST layer: when the catalog API is wired through RTK Query
 * in a later phase, the pages import from the API slice instead and this file is
 * deleted. No business rules live here — only display-ready sample data,
 * formatters and demo image URLs. None of the products, brands, counters or
 * promotions represent real inventory.
 *
 * Imagery: every image below is a static Unsplash CDN photo reference (free
 * license, no attribution required) with a fixed photo ID — nothing is
 * generated at runtime and no random placeholder services are used. Each ID
 * was chosen to match the product or category it illustrates. If an image
 * fails to load, every image container in index.css sits on a neutral
 * `--color-surface-alt` tile and the <img> alt text remains visible.
 */

export interface MockCategory {
  /** Slug used by the category filter in the UI. */
  id: string;
  name: string;
  tagline: string;
  itemCount: number;
  image: string;
}

export interface MockProduct {
  id: number;
  slug: string;
  name: string;
  brand: string;
  /** Matches {@link MockCategory.id}. */
  categoryId: string;
  price: number;
  /** Original price, when the product is discounted. */
  compareAtPrice?: number;
  rating: number;
  reviewCount: number;
  inStock: boolean;
  badge?: 'New' | 'Best seller' | 'Limited';
  shortDescription: string;
  description: string;
  highlights: string[];
  /** Gallery images; the first one is used as the card thumbnail. */
  images: string[];
  trending?: boolean;
  bestSeller?: boolean;
}

/**
 * Deterministic image URL for a fixed Unsplash photo ID. The mapping itself is
 * static — this helper only formats the CDN query (size/quality); it never
 * picks a photo at runtime.
 */
export function unsplash(id: string, width = 900): string {
  return `https://images.unsplash.com/photo-${id}?q=80&w=${width}&auto=format&fit=crop`;
}

/** Fixed photo IDs, one curated shot per product/category/hero slot. */
export const IMG = {
  // Audio & Headphones
  headphonesMain: '1505740420928-5e560c06d30e',
  headphonesAlt: '1484704849700-f032a568e944',
  headphonesThird: '1583394838336-acd977736f90',
  headphonesCategory: '1546435770-a3e426bf472b',
  speakerMain: '1545454675-3531b543be5d',
  speakerAlt: '1608043152269-423dbba4e7e1',
  speakerThird: '1608156639585-b3a032ef9689',
  soundbarMain: '1558537348-c0f8e733989d',
  // Wearables
  smartwatchMain: '1523275335684-37898b6baf30',
  smartwatchAlt: '1579586337278-3befd40fd17a',
  smartwatchThird: '1508685096489-7aacd43bd3b1',
  // Home & Kitchen
  cookwareMain: '1556909114-f6e7ad7d3136',
  cookwareAlt: '1574269909862-7e1d70bb8078',
  cookwareThird: '1556910103-1c02745aae4d',
  kettleMain: '1517668808822-9ebb02f2a0e6',
  kettleAlt: '1461023058943-07fcbe16d735',
  kettleThird: '1495474472287-4d71bcdd2085',
  applianceMain: '1570222094114-d054a817e56b',
  applianceAlt: '1556911220-e15b29be8c8f',
  applianceThird: '1610701596007-11502861dcfa',
  kitchenCategory: '1600585152220-90363fe7e115',
  // Fitness
  dumbbellMain: '1517836357463-d25dfeac3438',
  dumbbellAlt: '1584735935682-2f2b69dff9d2',
  yogaMatMain: '1592432678016-e910b452f9a2',
  yogaMatAlt: '1599901860904-17e6ed7083a0',
  yogaMatThird: '1601925260368-ae2f83cf8b7f',
  fitnessCategory: '1584735935682-2f2b69dff9d2',
  // Workspace
  deskMain: '1593062096033-9a26b09da705',
  deskAlt: '1524758631624-e2822e304c36',
  deskThird: '1518455027359-f3f8164ba6bd',
  deskCategory: '1541558869434-2840d308329a',
  lampMain: '1507473885765-e6ed057f782c',
  lampAlt: '1513506003901-1e6a229e2d15',
  monitorMain: '1527443224154-c4a3942d3acf',
  monitorAlt: '1547394765-185e1e68f34e',
  monitorThird: '1586210579191-33b45e38fa2c',
  // Outdoors
  bootsMain: '1520639888713-7851133b1ed0',
  bootsAlt: '1608256246200-53e635b5b65f',
  bootsThird: '1543163521-1bf539c55dd2',
  bagMain: '1553062407-98eeb64c6a62',
  bagAlt: '1581605405669-fcdf81165afa',
  outdoorsCategory: '1608256246200-53e635b5b65f',
  // Homepage hero
  heroWorkspace: '1531297484001-80022131f5a1',
  heroAudio: '1484704849700-f032a568e944',
  heroHome: '1600585152220-90363fe7e115',
} as const;

/** Product gallery: the main shot first, then coherent alternate angles. */
function gallery(...ids: string[]): string[] {
  return ids.map((id) => unsplash(id));
}

/** Mock counters for the navbar badges until the cart/wishlist APIs exist. */
export const MOCK_CART_ITEM_COUNT = 3;
export const MOCK_WISHLIST_ITEM_COUNT = 5;

const CURRENCY = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function formatPrice(value: number): string {
  return CURRENCY.format(value);
}

/** Discount percentage for a discounted product, otherwise `null`. */
export function discountPercent(product: MockProduct): number | null {
  if (!product.compareAtPrice || product.compareAtPrice <= product.price) {
    return null;
  }
  return Math.round(((product.compareAtPrice - product.price) / product.compareAtPrice) * 100);
}

export const MOCK_CATEGORIES: MockCategory[] = [
  {
    id: 'audio',
    name: 'Audio & Headphones',
    tagline: 'Studio sound for everyday listening',
    itemCount: 128,
    image: unsplash(IMG.headphonesCategory, 640),
  },
  {
    id: 'wearables',
    name: 'Wearables',
    tagline: 'Track health, sleep and training',
    itemCount: 94,
    image: unsplash(IMG.smartwatchAlt, 640),
  },
  {
    id: 'home',
    name: 'Home & Kitchen',
    tagline: 'Everyday upgrades for the home',
    itemCount: 212,
    image: unsplash(IMG.kitchenCategory, 640),
  },
  {
    id: 'fitness',
    name: 'Fitness',
    tagline: 'Gear that keeps the routine going',
    itemCount: 76,
    image: unsplash(IMG.fitnessCategory, 640),
  },
  {
    id: 'workspace',
    name: 'Workspace',
    tagline: 'Desks, lighting and accessories',
    itemCount: 143,
    image: unsplash(IMG.deskCategory, 640),
  },
  {
    id: 'outdoors',
    name: 'Outdoors',
    tagline: 'Ready for the weekend away',
    itemCount: 88,
    image: unsplash(IMG.outdoorsCategory, 640),
  },
];

export const MOCK_PRODUCTS: MockProduct[] = [
  {
    id: 1,
    slug: 'aerowave-pro-headphones',
    name: 'AeroWave Pro Wireless Headphones',
    brand: 'Northline Audio',
    categoryId: 'audio',
    price: 189.0,
    compareAtPrice: 249.0,
    rating: 4.8,
    reviewCount: 412,
    inStock: true,
    badge: 'Best seller',
    trending: true,
    bestSeller: true,
    shortDescription: 'Adaptive noise cancelling with 38-hour battery life.',
    description:
      'Over-ear headphones tuned for long listening sessions. Adaptive noise cancelling adjusts to your surroundings, while memory-foam earcups and a lightweight frame stay comfortable through a full workday.',
    highlights: [
      'Adaptive noise cancelling with transparency mode',
      '38 hours of playback, 5-minute quick charge',
      'Multipoint pairing for two devices at once',
    ],
    images: gallery(IMG.headphonesMain, IMG.headphonesAlt, IMG.headphonesThird),
  },
  {
    id: 2,
    slug: 'pulsefit-tracker-3',
    name: 'PulseFit Tracker 3',
    brand: 'Kinetic Labs',
    categoryId: 'wearables',
    price: 119.5,
    compareAtPrice: 159.0,
    rating: 4.6,
    reviewCount: 268,
    inStock: true,
    trending: true,
    bestSeller: true,
    shortDescription: 'Sleep, recovery and heart-rate tracking on a bright AMOLED screen.',
    description:
      'A slim fitness tracker that keeps its promises: accurate heart-rate and sleep staging, a ten-day battery and a swim-safe build. Syncs with the major health platforms.',
    highlights: ['AMOLED always-on display', 'Up to 10 days battery life', 'Water resistant to 50 m'],
    images: gallery(IMG.smartwatchMain, IMG.smartwatchAlt, IMG.smartwatchThird),
  },
  {
    id: 3,
    slug: 'emberstone-skillet-12',
    name: 'EmberStone Cast Skillet, 12 inch',
    brand: 'EmberStone',
    categoryId: 'home',
    price: 74.0,
    rating: 4.7,
    reviewCount: 531,
    inStock: true,
    bestSeller: true,
    shortDescription: 'Pre-seasoned cast iron that goes from hob to oven.',
    description:
      'A pre-seasoned cast skillet with a smooth cooking surface and a helper handle for two-handed lifting. Built to be handed down rather than replaced.',
    highlights: ['Pre-seasoned, oven safe to 260 C', 'Smooth pour spouts on both sides', 'Lifetime manufacturer warranty'],
    images: gallery(IMG.cookwareMain, IMG.cookwareAlt, IMG.cookwareThird),
  },
  {
    id: 4,
    slug: 'meridian-standing-desk',
    name: 'Meridian Electric Standing Desk',
    brand: 'Meridian Works',
    categoryId: 'workspace',
    price: 429.0,
    compareAtPrice: 519.0,
    rating: 4.5,
    reviewCount: 184,
    inStock: true,
    trending: true,
    shortDescription: 'Dual-motor frame with four memory presets.',
    description:
      'Height-adjustable desk with a dual-motor frame, anti-collision sensors and a memory keypad. The 120 x 60 cm bamboo top handles two monitors comfortably.',
    highlights: ['Dual motors, 60-125 cm range', 'Four memory presets plus anti-collision', 'Cable tray included'],
    images: gallery(IMG.deskMain, IMG.deskAlt, IMG.deskThird),
  },
  {
    id: 5,
    slug: 'trailmark-hiking-boots',
    name: 'Trailmark Waterproof Hiking Boots',
    brand: 'Trailmark',
    categoryId: 'outdoors',
    price: 165.0,
    compareAtPrice: 199.0,
    rating: 4.4,
    reviewCount: 143,
    inStock: true,
    badge: 'Limited',
    shortDescription: 'Waterproof leather boots with a grippy lug sole.',
    description:
      'Full-grain leather boots with a waterproof membrane and a cushioned midsole, cut for long days on uneven ground.',
    highlights: ['Waterproof breathable membrane', 'Deep lug outsole', 'Reinforced toe cap'],
    images: gallery(IMG.bootsMain, IMG.bootsAlt, IMG.bootsThird),
  },
  {
    id: 6,
    slug: 'lumen-desk-lamp',
    name: 'Lumen Adjustable Desk Lamp',
    brand: 'Lumen Studio',
    categoryId: 'workspace',
    price: 84.0,
    rating: 4.3,
    reviewCount: 97,
    inStock: true,
    shortDescription: 'Flicker-free LED light with five temperature settings.',
    description:
      'A weighted desk lamp with a wide light bar, stepless dimming and five colour temperatures for reading, editing or video calls.',
    highlights: ['Flicker-free, CRI 95 LEDs', 'Stepless dimming, five temperatures', 'USB-C pass-through port'],
    images: gallery(IMG.lampMain, IMG.lampAlt, IMG.deskCategory),
  },
  {
    id: 7,
    slug: 'corelift-adjustable-dumbbells',
    name: 'CoreLift Adjustable Dumbbell Pair',
    brand: 'CoreLift',
    categoryId: 'fitness',
    price: 249.0,
    compareAtPrice: 299.0,
    rating: 4.6,
    reviewCount: 221,
    inStock: true,
    trending: true,
    shortDescription: 'Two dumbbells that replace a rack, 2.5-24 kg each.',
    description:
      'Dial-adjustable dumbbells with a secure locking mechanism: go from warm-up weights to working sets in seconds without leaving the mat.',
    highlights: ['2.5-24 kg per dumbbell', 'Fast dial selection mechanism', 'Compact storage trays included'],
    images: gallery(IMG.dumbbellMain, IMG.dumbbellAlt, IMG.yogaMatThird),
  },
  {
    id: 8,
    slug: 'brewhaus-precision-kettle',
    name: 'BrewHaus Precision Kettle',
    brand: 'BrewHaus',
    categoryId: 'home',
    price: 96.0,
    rating: 4.7,
    reviewCount: 356,
    inStock: true,
    bestSeller: true,
    shortDescription: 'Variable temperature control, accurate to within 1 C.',
    description:
      'Gooseneck kettle with variable temperature control and a hold function, so pour-over coffee and green tea both get the water they want.',
    highlights: ['Temperature steps of 1 C', '60-minute hold at set temperature', 'Stainless gooseneck spout'],
    images: gallery(IMG.kettleMain, IMG.kettleAlt, IMG.kettleThird),
  },
  {
    id: 9,
    slug: 'soundbar-lite-2',
    name: 'SoundBar Lite 2 Sound System',
    brand: 'Northline Audio',
    categoryId: 'audio',
    price: 139.0,
    compareAtPrice: 179.0,
    rating: 4.2,
    reviewCount: 118,
    inStock: true,
    shortDescription: 'Compact soundbar with a wireless subwoofer.',
    description:
      'A two-piece sound system for smaller rooms: clear dialogue from the bar, weight from the wireless sub. HDMI eARC plus optical and Bluetooth.',
    highlights: ['Wireless subwoofer included', 'HDMI eARC and optical inputs', 'Dialogue clarity mode'],
    images: gallery(IMG.soundbarMain, IMG.speakerAlt, IMG.speakerThird),
  },
  {
    id: 10,
    slug: 'vertex-yoga-mat',
    name: 'Vertex Grip Yoga Mat',
    brand: 'Vertex Motion',
    categoryId: 'fitness',
    price: 58.0,
    rating: 4.5,
    reviewCount: 264,
    inStock: true,
    trending: true,
    shortDescription: '6 mm cushioning with a non-slip natural rubber base.',
    description:
      'A dense, grippy mat that stays put during flow work and still cushions knees and wrists. The closed-cell surface wipes clean in seconds.',
    highlights: ['6 mm of joint-friendly cushioning', 'Non-slip on both sides', 'Latex-free closed-cell surface'],
    images: gallery(IMG.yogaMatMain, IMG.yogaMatAlt, IMG.yogaMatThird),
  },
  {
    id: 11,
    slug: 'atlas-weekender-bag',
    name: 'Atlas 40L Weekender Bag',
    brand: 'Atlas Supply',
    categoryId: 'outdoors',
    price: 148.0,
    compareAtPrice: 185.0,
    rating: 4.8,
    reviewCount: 176,
    inStock: true,
    badge: 'New',
    shortDescription: 'Carry-on sized, weatherproof and built for two nights away.',
    description:
      'A structured weekender in waxed canvas with leather reinforcements, a separate shoe compartment and a padded laptop sleeve.',
    highlights: ['40 L carry-on capacity', 'Weatherproof waxed canvas', 'Separate shoe pocket'],
    images: gallery(IMG.bagMain, IMG.bagAlt, IMG.bootsThird),
  },
  {
    id: 12,
    slug: 'clarity-4k-monitor-27',
    name: 'Clarity 27-inch 4K Monitor',
    brand: 'Clarity Display',
    categoryId: 'workspace',
    price: 379.0,
    compareAtPrice: 449.0,
    rating: 4.4,
    reviewCount: 205,
    inStock: true,
    trending: true,
    shortDescription: '27-inch 4K IPS panel with a single-cable USB-C dock.',
    description:
      'A 4K IPS display with factory colour calibration and a 90 W USB-C connection that powers a laptop while carrying video and data.',
    highlights: ['3840 x 2160 IPS, 99% sRGB', 'Single-cable 90 W USB-C dock', 'Height, tilt and pivot stand'],
    images: gallery(IMG.monitorMain, IMG.monitorAlt, IMG.monitorThird),
  },
  {
    id: 13,
    slug: 'orbit-smart-speaker',
    name: 'Orbit Smart Speaker',
    brand: 'Northline Audio',
    categoryId: 'audio',
    price: 89.0,
    rating: 4.1,
    reviewCount: 84,
    inStock: true,
    shortDescription: 'Room-filling 360-degree sound in a compact body.',
    description:
      'A small speaker that fills a room: 360-degree driver array, stereo pairing and voice control for the music you already use.',
    highlights: ['360-degree sound with stereo pairing', 'Voice assistant support', 'Wi-Fi and Bluetooth 5.3'],
    images: gallery(IMG.speakerMain, IMG.speakerAlt, IMG.speakerThird),
  },
  {
    id: 14,
    slug: 'harvest-air-fryer-xl',
    name: 'Harvest XL Air Fryer',
    brand: 'Harvest Kitchen',
    categoryId: 'home',
    price: 129.0,
    compareAtPrice: 169.0,
    rating: 4.6,
    reviewCount: 618,
    inStock: false,
    bestSeller: true,
    shortDescription: '6.5 L basket with eight one-touch cooking presets.',
    description:
      'A family-sized air fryer with a dishwasher-safe basket, eight presets and a shake reminder that actually helps chips come out even.',
    highlights: ['6.5 L capacity, serves 4-6', 'Eight one-touch presets', 'Dishwasher-safe basket'],
    images: gallery(IMG.applianceMain, IMG.applianceAlt, IMG.applianceThird),
  },
];

export function findMockProductById(id: number): MockProduct | undefined {
  return MOCK_PRODUCTS.find((product) => product.id === id);
}

export function findMockProductBySlug(slug: string): MockProduct | undefined {
  return MOCK_PRODUCTS.find((product) => product.slug === slug);
}

export function getTrendingProducts(limit = 6): MockProduct[] {
  return MOCK_PRODUCTS.filter((product) => product.trending).slice(0, limit);
}

export function getBestSellerProducts(limit = 4): MockProduct[] {
  return MOCK_PRODUCTS.filter((product) => product.bestSeller).slice(0, limit);
}

export function getProductsByCategory(categoryId: string, limit = 4): MockProduct[] {
  return MOCK_PRODUCTS.filter((product) => product.categoryId === categoryId).slice(0, limit);
}

export function findCategoryById(categoryId: string): MockCategory | undefined {
  return MOCK_CATEGORIES.find((category) => category.id === categoryId);
}

/** First gallery image, used as the card thumbnail. */
export function primaryImage(product: MockProduct): string {
  return product.images[0] ?? unsplash(IMG.headphonesMain);
}