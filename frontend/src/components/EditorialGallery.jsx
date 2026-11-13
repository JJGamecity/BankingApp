import { useState } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";

const scenes = [
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

export default function EditorialGallery({ compact = false }) {
  const [index, setIndex] = useState(0);
  const scene = scenes[index];
  return (
    <section
      className={`editorial-gallery ${compact ? "gallery-compact" : ""}`}
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
