import { useState } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";

const defaultScenes = [
  {
    image: "/images/everyday-banking.svg?v=light-blue-2",
    alt: "A light blue banking card beside a payment terminal on a pale blue background",
    label: "YOUR EVERYDAY, SIMPLIFIED",
    title: "A little more ease. Every day.",
    caption: "Your money, with a fresh perspective.",
  },
  {
    image: "/images/everyday-banking.svg",
    alt: "An illustrated banking card beside a contactless payment terminal",
    label: "YOUR EVERYDAY, SIMPLIFIED",
    title: "A little more ease. Every day.",
    caption: "Your money, with a fresh perspective.",
  },
  {
    image: "/images/green-architecture.jpg",
    alt: "Modern architecture with lush planting on every balcony",
    label: "ROOM TO GROW",
    title: "Build a life that feels like yours.",
    caption: "A little perspective for your everyday.",
  },
  {
    image: "/images/alpine-lake.jpg",
    alt: "A quiet alpine lake surrounded by forested mountains",
    label: "A FRESH PERSPECTIVE",
    title: "Make space for what matters.",
    caption: "Your next chapter is yours to imagine.",
  },
];

const blueScenes = [
  {
    image: "/images/blue-architecture.jpg",
    alt: "Blue glass towers rising into a clear blue sky",
    label: "A CLEARER OUTLOOK",
    title: "A fresh perspective starts here.",
    caption: "A little clarity for your next chapter.",
  },
  {
    image: "/images/blue-city.jpg",
    alt: "Curved blue glass building reflecting the sky",
    label: "BUILT AROUND YOU",
    title: "Room for your bigger picture.",
    caption: "Your everyday banking, thoughtfully brought together.",
  },
];

export default function EditorialGallery({ compact = false, blue = false }) {
  const scenes = blue ? blueScenes : defaultScenes;
  const [index, setIndex] = useState(0);
  const scene = scenes[index];
  return (
    <section
      className={`editorial-gallery ${compact ? "gallery-compact" : ""} ${blue ? "gallery-blue" : ""}`}
      aria-label="Everyday inspiration"
    >
      <img
        key={scene.image}
        className="editorial-image"
        src={scene.image}
        alt={scene.alt}
        width="1200"
        height="800"
        loading="lazy"
      />
      <div className="editorial-shade" />
      <div className="editorial-copy" aria-live="polite">
        <span className="eyebrow">{scene.label}</span>
        <h2>{scene.title}</h2>
        <p>{scene.caption}</p>
      </div>
      <div className="gallery-controls">
        <div className="gallery-dots">
          {scenes.map((item, position) => (
            <button
              key={item.image}
              type="button"
              aria-label={`Show ${item.label.toLowerCase()}`}
              aria-pressed={position === index}
              onClick={() => setIndex(position)}
            />
          ))}
        </div>
        <div className="gallery-arrows">
          <button
            type="button"
            aria-label="Previous image"
            onClick={() =>
              setIndex((index + scenes.length - 1) % scenes.length)
            }
          >
            <ArrowLeft size={17} />
          </button>
          <button
            type="button"
            aria-label="Next image"
            onClick={() => setIndex((index + 1) % scenes.length)}
          >
            <ArrowRight size={17} />
          </button>
        </div>
      </div>
    </section>
  );
}
