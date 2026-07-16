import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium",
  {
    variants: {
      variant: {
        default: "border-astra-border bg-astra-surface text-astra-text-secondary",
        accent: "border-astra-accent/30 bg-astra-accent/10 text-astra-accent",
        success: "border-astra-success/30 bg-astra-success/10 text-astra-success",
        warning: "border-astra-warning/30 bg-astra-warning/10 text-astra-warning",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

function Badge({
  className,
  variant,
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return (
    <span className={cn(badgeVariants({ variant, className }))} {...props} />
  );
}

export { Badge, badgeVariants };
