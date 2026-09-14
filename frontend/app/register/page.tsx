"use client";

import { extractErrorMessage, useAuth } from "@/lib/auth-context";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

const registerSchema = z.object({
  email: z.string().email("Geçerli bir email adresi girin"),
  password: z.string().min(8, "Şifre en az 8 karakter olmalı"),
  displayName: z.string().min(1, "İsim gerekli"),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const { register: registerUser } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  async function onSubmit(values: RegisterFormValues) {
    setFormError(null);
    try {
      await registerUser(values.email, values.password, values.displayName);
      router.push("/groups");
    } catch (err) {
      setFormError(extractErrorMessage(err));
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-4 px-6">
      <h1 className="text-2xl font-semibold">Kayıt Ol</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="displayName" className="text-sm font-medium">
            İsim
          </label>
          <input
            id="displayName"
            type="text"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("displayName")}
          />
          {errors.displayName && <p className="text-sm text-red-600">{errors.displayName.message}</p>}
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="email" className="text-sm font-medium">
            Email
          </label>
          <input
            id="email"
            type="email"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("email")}
          />
          {errors.email && <p className="text-sm text-red-600">{errors.email.message}</p>}
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="password" className="text-sm font-medium">
            Şifre
          </label>
          <input
            id="password"
            type="password"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("password")}
          />
          {errors.password && <p className="text-sm text-red-600">{errors.password.message}</p>}
        </div>
        {formError && <p className="text-sm text-red-600">{formError}</p>}
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded bg-foreground px-4 py-2 text-background disabled:opacity-50"
        >
          Kayıt Ol
        </button>
      </form>
    </div>
  );
}
