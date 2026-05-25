<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('presences', function (Blueprint $table) {
    $table->id();
    $table->foreignId('user_id')->constrained('users');
    $table->string('subject'); // Ubah dari foreignId menjadi string
    $table->string('teacher'); // Tambahkan kolom teacher (string)
    $table->enum('status', ['hadir', 'terlambat'])->default('hadir');
    $table->decimal('latitude', 10, 8);
    $table->decimal('longitude', 11, 8);
    $table->timestamps();
});
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('presences');
    }
};
